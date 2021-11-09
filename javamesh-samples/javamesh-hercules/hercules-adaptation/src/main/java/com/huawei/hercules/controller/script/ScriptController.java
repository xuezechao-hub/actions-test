package com.huawei.hercules.controller.script;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huawei.hercules.controller.BaseController;
import com.huawei.hercules.service.perftest.IPerftestService;
import com.huawei.hercules.service.script.IScripService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ScriptController extends BaseController {
    @Autowired
    IScripService scripService;

    @Autowired
    IPerftestService perftestService;

    @Value("${decisionEngine.url}")
    private String host;

    /**
     * 新建脚本
     *
     * @param params 脚本信息
     * @return 脚本新建状态+路径
     */
    @RequestMapping(value = "/script", method = RequestMethod.POST)
    public JSONObject creat(@RequestBody JSONObject params) {
        if (params == null) {
            return returnError("参数缺失");
        }

        String path = params.getString("folder");
        String testUrl = params.getString("for_url");
        String fileName = params.getString("script_name");
        String scriptType = getScriptType(params.getString("language"));

        fileName = getAllfileName(fileName, scriptType);
        boolean createLibAndResources = false;
        if (params.get("has_resource") != null) {
            createLibAndResources = params.getBoolean("has_resource");
        }
        JSONObject options = new JSONObject();
        options.put("method", params.get("method"));
        options.put("headers", parseScript(params.getJSONArray("headers")));
        options.put("cookies", parseScriptCookies(params.getJSONArray("cookies")));
        options.put("params", parseScript(params.getJSONArray("params")));
        // 1.创建
        JSONObject creat = scripService.createForm(path, testUrl, fileName, scriptType, createLibAndResources, options.toJSONString());
        if (GROOVY_MAVEN_TYPE.equalsIgnoreCase(scriptType)) {
            if (creat.getBoolean("success")) {
                return returnSuccess();
            } else {
                return returnError(creat.get("exception").toString());
            }
        }
        JSONObject result = returnError();
        if (creat != null) {
            Map<String, Object> file = (Map<String, Object>) creat.get("file");
            // 2.保存
            Map<String, Object> properties = (Map<String, Object>) file.get("properties");
            JSONObject fileEntry = new JSONObject();
            fileEntry.put("path", file.get("path"));
            fileEntry.put("description", file.get("description"));
            fileEntry.put("content", file.get("content"));
            String targetHosts = properties == null ? null : properties.get("targetHosts").toString();
            String basePath = scripService.saveScript(parseScript(fileEntry.toString()), targetHosts, "0", createLibAndResources);
            result.put(JSON_RESULT_KEY, SUCCESS);
            result.put("basePath", basePath);
        }
        return result;
    }

    /**
     * 保存脚本[更新]
     *
     * @param params 更新内容
     * @return 更新状态
     */
    @RequestMapping(value = "/script", method = RequestMethod.PUT)
    public JSONObject saveScript(@RequestBody JSONObject params) {
        if (StringUtils.isEmpty(params.get("path"))) {
            return returnError("脚本路径为空");
        }
        // 保存脚本：非第一次创建createLibAndResource: false
        JSONObject fileEntry = new JSONObject();
        fileEntry.put("path", params.get("path"));
        fileEntry.put("description", params.get("commit"));
        fileEntry.put("content", params.get("script"));
        scripService.saveScript(parseScript(fileEntry.toString()), getTargetHosts(params.getString("path")), "0", false);
        return returnSuccess();
    }

    /**
     * 脚本验证
     *
     * @param params 脚本信息
     * @return 验证结果
     */
    @RequestMapping(value = "/script/check", method = RequestMethod.POST)
    public JSONObject validateScript(@RequestBody JSONObject params) {
        if (!params.containsKey("script") || !params.containsKey("path")) {
            return returnError();
        }
        JSONObject fileEntry = new JSONObject();
        fileEntry.put("path", params.get("path"));
        fileEntry.put("content", params.get("script"));
        HttpEntity<String> httpEntity = scripService.validateScript(fileEntry.toString(), getTargetHosts(params.getString("path")));
        String body = httpEntity.getBody();
        JSONObject jsonObject = returnSuccess();
        jsonObject.put("data", body);
        return jsonObject;
    }


    /**
     * 查询脚本列表
     *
     * @param pageSize 分页数【实际上返回所有数据】
     * @param keywords 查询关键字
     * @return 脚本列表
     */
    @RequestMapping(value = "/script", method = RequestMethod.GET)
    public JSONObject queryScriptList(@RequestParam(required = false) String folder, @RequestParam(required = false) int pageSize, @RequestParam(required = false) String keywords) {
        JSONObject result;
        if (StringUtils.isEmpty(keywords)) {
            result = scripService.getAllList(folder == null ? "" : folder);
        } else {
            result = scripService.search(keywords);
        }
        if (result != null) {
            List<Map<String, Object>> files = (List<Map<String, Object>>) result.get("files");
            result.put("total", files.size());
            for (Map<String, Object> file : files) {
                file.put("type", "DIR".equals(file.get("fileType")) ? "folder" : "file");
                file.put("script_name", file.get("fileName"));
                file.put("version", file.get("revision"));
                file.put("size", file.get("fileSize"));
                file.put("commit", file.get("description"));
                file.put("update_time", longToDate((Long) file.get("lastModifiedDate")));
            }
            result.put("data", files);
            result.remove("files");
        }
        return result;
    }

    /**
     * 脚本下拉列表查询
     *
     * @param value 脚本关键字
     * @return 脚本列表
     */
    @RequestMapping(value = "/script/search", method = RequestMethod.GET)
    public JSONObject searchScripts(@RequestParam(required = false) String value) {
        JSONObject scripts = new JSONObject();
        HttpEntity<String> allScripts = perftestService.getScripts("");
        String body = allScripts.getBody();
        List<Map<String, Object>> allPaths = JSONObject.parseObject(body, List.class);
        List<String> paths = new ArrayList<>();
        if (allPaths != null && !allPaths.isEmpty()) {
            for (Map<String, Object> row : allPaths) {
                String path = row.get("path").toString();
                if (StringUtils.isEmpty(value) || path.contains(value)) {
                    paths.add(path);
                }
            }
        }
        scripts.put("data", paths);
        return scripts;
    }

    @RequestMapping(value = "/script/folder", method = RequestMethod.POST)
    public JSONObject addFolder(@RequestBody JSONObject params) {
        if (params == null) {
            return returnError("参数缺失");
        }
        String path = params.getString("folder");
        String folder = params.getString("folder_name");
        String spath = scripService.addFolder(path, folder);
        JSONObject jsonObject = returnSuccess();
        jsonObject.put("path", spath);
        return jsonObject;
    }

    @RequestMapping(value = "/script", method = RequestMethod.DELETE)
    public JSONObject delete(@RequestParam(required = false) String folder, @RequestParam(name = "script_name[]") String[] script_name) {
        if (StringUtils.isEmpty(script_name)) {
            return returnError();
        }
        try {
            scripService.delete(folder, arrayToStr(script_name));
        } catch (Exception e) {
            return returnError("脚本删除失败");
        }
        return returnSuccess();
    }

    /**
     * 上传脚本
     *
     * @param request request
     * @param folder 路径
     * @param commit 提交信息
     * @param file 文件
     * @return 上传结果状态
     * @throws IOException 异常
     */
    @RequestMapping(value = "/script/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JSONObject uploadFile(HttpServletRequest request, @RequestParam(required = false) String folder, @RequestParam String commit,
                                 @RequestParam("file") MultipartFile file) throws IOException {
        String url = host + "/rest/script/uploadFile";//服务端要调用的外部接口
        //httpclients构造post请求
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);
            // 设置header
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    Enumeration<String> headers = request.getHeaders(name);
                    if ("cookie".equalsIgnoreCase(name)) {
                        while (headers.hasMoreElements()) {
                            String value = headers.nextElement();
                            httpPost.addHeader(name, value);
                        }
                    }
                }
            }
            //HttpMultipartMode.RFC6532参数的设定是为避免文件名为中文时乱码
            MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);
            String originFileName = file.getOriginalFilename();
            builder.addBinaryBody("uploadFile", file.getBytes(), ContentType.MULTIPART_FORM_DATA, originFileName);
            ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
            StringBody contentBody = new StringBody(commit,contentType);
            builder.addPart("description", contentBody);
            contentBody = new StringBody(folder,contentType);
            builder.addPart("path", contentBody);
            builder.setCharset(MIME.UTF8_CHARSET);
            org.apache.http.HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);// 执行提交
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return returnSuccess();
            } else {
                return returnError("上传文件失败");
            }
        } finally {//处理结束后关闭httpclient的链接
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping("script/download")
    public void downloadFile(@RequestParam(required = false) String path, HttpServletResponse response) throws Exception {
        JSONObject jsonObject = scripService.downloadFile(path);
        if (jsonObject == null || !jsonObject.getBoolean(JSON_RESULT_KEY)) {
            return;
        }
        downloadFile(jsonObject, response);
    }

    /**
     * 查看脚本
     *
     * @param path 路径
     * @param revision 版本
     * @return 脚本信息
     */
    @RequestMapping(value = "/script/get", method = RequestMethod.GET)
    public JSONObject getScript(@RequestParam(required = false) String path,
                                @RequestParam(required = false) Long revision) {
        JSONObject jsonObject = scripService.getOne(path, revision);
        JSONObject result = new JSONObject();
        Map<String, Object> file = (Map<String, Object>) jsonObject.get("file");
        if (file != null) {
            file.put("script", file.get("content"));
            file.put("commit", file.get("description"));
            file.remove("content");

            // 查询资源文件
            HttpEntity<String> resources = perftestService.getResources(path, "");
            JSONObject body = JSONObject.parseObject(resources.getBody());
            file.put("script_resource", body.get("resources"));
            file.put("targetHosts", body.get("targetHosts"));
        }
        result.put("data", file);
        return result;
    }


    /**
     * 查询脚本的主机
     *
     * @param path 路径
     * @return 主机信息
     */
    @RequestMapping(value = "/script/host", method = RequestMethod.GET)
    public JSONObject getScriptHost(@RequestParam String path) {
        JSONObject result = new JSONObject();
        List<Map<String, Object>> targetHosts = new LinkedList<>();
        // 查询资源文件
        HttpEntity<String> resources = perftestService.getResources(path, "");
        JSONObject body = JSONObject.parseObject(resources.getBody());
        String hosts = body.getString("targetHosts");
        if (!StringUtils.isEmpty(hosts)) {
            String[] allHosts = hosts.split(",");
            int i = 1;
            for (String host : allHosts) {
                Map<String, Object> item = new HashMap<>();
                item.put("host_id", i++);
                item.put("domain", host);
                targetHosts.add(item);
            }
        }
        result.put("data", targetHosts);
        return result;
    }

    /**
     * 添加主机
     *
     * @param params 主机信息
     * @return 添加结果状态
     */
    @RequestMapping(value = "/script/host", method = RequestMethod.POST)
    public JSONObject addScriptHost(@RequestBody JSONObject params) {
        String path = params.getString("path");
        String domain = params.getString("domain");
        String ip = params.getString("ip");
        // 1.查询脚本
        JSONObject script = getScript(path, null);
        Map<String, Object> files = (Map<String, Object>) script.get("data");
        // 2.保存脚本：非第一次创建createLibAndResource: false
        JSONObject fileEntry = new JSONObject();
        fileEntry.put("path", path);
        fileEntry.put("description", files.get("description"));
        fileEntry.put("content", files.get("script"));
        String targetHosts = files.get("targetHosts").toString() + "," + getHost(domain, ip);
        scripService.saveScript(parseScript(fileEntry.toString()), targetHosts, "0", false);
        return returnSuccess();
    }

    /**
     * 删除主机
     *
     * @param host_id 主机地址
     * @param path 路径
     * @return 删除状态
     */
    @RequestMapping(value = "/script/host", method = RequestMethod.DELETE)
    public JSONObject deleteScriptHost(@RequestParam Integer host_id, @RequestParam String path) {
        if (host_id == null || host_id <= 0) {
            return returnError("删除的主机不存在");
        }
        // 1.查询脚本
        JSONObject script = getScript(path, null);
        Map<String, Object> files = (Map<String, Object>) script.get("data");
        String targetHosts = files.get("targetHosts").toString();
        String[] allHosts = targetHosts.split(",");
        if (host_id > allHosts.length) {
            return returnError("删除的主机不存在");
        }

        List<String> list = new ArrayList<>(Arrays.asList(allHosts));
        list.remove(host_id - 1);
        targetHosts = arrayToStr(list);
        // 2.保存脚本：非第一次创建createLibAndResource: false
        JSONObject fileEntry = new JSONObject();
        fileEntry.put("path", path);
        fileEntry.put("description", files.get("description"));
        fileEntry.put("content", files.get("script"));

        scripService.saveScript(parseScript(fileEntry.toString()), targetHosts, "0", false);
        return returnSuccess();
    }

    private JSONArray parseScript(JSONArray array) {
        JSONArray target = new JSONArray();
        if (array != null && !array.isEmpty()) {
            for (Object item : array) {
                JSONObject current = (JSONObject) item;
                JSONObject thisItem = new JSONObject();
                thisItem.put("name", current.get("key"));
                thisItem.put("value", current.get("value"));
                target.add(thisItem);
            }
        }
        return target;
    }

    private JSONArray parseScriptCookies(JSONArray array) {
        JSONArray target = new JSONArray();
        if (array != null && !array.isEmpty()) {
            for (Object item : array) {
                JSONObject current = (JSONObject) item;
                JSONObject thisItem = new JSONObject();
                thisItem.put("name", current.get("key"));
                thisItem.put("value", current.get("value"));
                thisItem.put("domain", current.get("value_a"));
                thisItem.put("path", current.get("value_b"));
                target.add(thisItem);
            }
        }
        return target;
    }

    private String getAllfileName(String fileName, String scriptType) {
        if ("jython".equalsIgnoreCase(scriptType) && !fileName.endsWith(".py")) {
            fileName = fileName + ".py";
        } else if ("groovy".equalsIgnoreCase(scriptType) && !fileName.endsWith("groovy")) {
            fileName = fileName + ".groovy";
        }
        return fileName;
    }

    private String getScriptType(String scriptType) {
        if ("jython".equalsIgnoreCase(scriptType) || "groovy".equalsIgnoreCase(scriptType)) {
            return scriptType.toLowerCase().trim();
        } else if ("Groovy Maven Project".equalsIgnoreCase(scriptType)) {
            return GROOVY_MAVEN_TYPE;
        }
        return null;
    }

    /**
     * 查询脚本主机
     *
     * @param path 路径
     * @return 主机信息
     */
    private String getTargetHosts(String path) {
        if (StringUtils.isEmpty(path)) {
            return "";
        }
        HttpEntity<String> resources = perftestService.getResources(path, "");
        JSONObject body = JSONObject.parseObject(resources.getBody());
        return body.getString("targetHosts");
    }
}
