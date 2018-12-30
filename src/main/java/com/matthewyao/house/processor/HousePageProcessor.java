package com.matthewyao.house.processor;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpStatus;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.HttpClientDownloader;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.SimpleProxyProvider;
import us.codecraft.webmagic.selector.Selectable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: yaokuan
 * @Date: 2018/12/30 下午2:56
 */
public class HousePageProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(600).setTimeOut(10000);

    private static final int TOTAL_SPIDER_PAGE = 13;

    private static final int TOTAL_PROXY_SIZE = 50;

    private static File file = new File("/Users/yaokuan/Mine/house/result");


    @Override
    public void process(Page page) {
        if (page.getUrl().regex(".*index.*").match()) {
            String baseUrl = "http://www.shcngz.com/pages/news_list.aspx?mid=29&pageid=";
            List<String> urls = new ArrayList<>();
            for (int i = 1; i <= TOTAL_SPIDER_PAGE; i++) {
                urls.add(baseUrl + i);
            }
            page.addTargetRequests(urls);
        }else {
            String type = page.getUrl().regex(".*news_(\\w+).aspx.*").toString();
            if (type.equals("list")) {
                List<String> allLinks = page.getHtml()
                        .$(".news_list_ul").links()
                        .regex(".*news_detail.*")
                        .all();
                if (CollectionUtils.isNotEmpty(allLinks)) {
                    page.addTargetRequests(allLinks);
                }
            } else if (type.equals("detail")) {
                Selectable content = page.getHtml().$(".news_detail");
                boolean need = content.$("h4").regex(".*(选房的通知|房源供应).*").match();
                String pageId = page.getUrl().regex(".*newsid=(\\d*)").toString();
                if (need) {
                    String date = content.$("h5").regex(".*(\\d{4}\\-\\d{2}\\-\\d{2}).*").toString();
                    String name = content.regex(".*(.{2}(公寓|小区)|古北路项目|华东化工大厦).*").toString();
                    String count = content.regex("[\\u4e00-\\u9fa5|>](\\d+)(<.*>)?套房源").toString();
                    System.out.println(String.format(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> %s %s %s %s", pageId, date, name, count));
                    StringBuffer sb = new StringBuffer();
                    sb.append(pageId).append("\t").append(date).append("\t").append(name).append("\t").append(count).append("\n");
                    writeToFile(sb);
                }else {
                    StringBuffer sb = new StringBuffer();
                    sb.append(pageId).append("\t").append("无关页面\n");
                    writeToFile(sb);
                }
            }
        }
    }

    private void writeToFile(StringBuffer sb) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file, true);
            fw.append(sb.toString());
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    private static HttpClientDownloader getHttpClientDownloader() throws IOException {
        //先获取代理
        HttpClientDownloader downloader = new HttpClientDownloader();
        HttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("https://raw.githubusercontent.com/fate0/proxylist/master/proxy.list#");
        HttpResponse response = client.execute(httpGet);
        List<Proxy> validProxyList = new ArrayList<>();
        if (response.getStatusLine().getStatusCode() == HttpStatus.OK.value()) {
            //获取代理成功
            InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
            BufferedReader br = new BufferedReader(isr);
            for (int i = 0; i <= TOTAL_PROXY_SIZE; i++) {
                String line = br.readLine();
                JSONObject obj = JSONObject.parseObject(line);
                String host = obj.getString("host");
                int port = obj.getIntValue("port");
                System.out.println("valid proxy, host: " + host + " ,port: " + port);
                Proxy proxy = new Proxy(host, port);
                validProxyList.add(proxy);
            }
        }
        downloader.setProxyProvider(new SimpleProxyProvider(validProxyList));
        return downloader;
    }

    public static void main(String[] args) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        HttpClientDownloader downloader = getHttpClientDownloader();

        Spider.create(new HousePageProcessor())
//                .addUrl("http://www.shcngz.com/pages/news_detail.aspx?newsid=738")
                .addUrl("http://www.shcngz.com/pages/index.aspx")
                .setDownloader(downloader)
                .thread(3).run();
    }
}
