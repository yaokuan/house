package com.matthewyao.house.processor;

import com.alibaba.fastjson.JSONObject;
import com.matthewyao.house.util.MailConfig;
import com.matthewyao.house.util.MailUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: yaokuan
 * @Date: 2018/12/30 下午2:56
 */
public class NewHouseProcessor implements PageProcessor {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(600).setTimeOut(10000);

    private static final int TOTAL_PROXY_SIZE = 20;

    @Override
    public void process(Page page) {
            String type = page.getUrl().regex(".*news_(\\w+).aspx.*").toString();
            if (type.equals("list")) {
                List<String> allLinks = page.getHtml()
                        .$(".news_list_ul").links()
                        .regex(".*news_detail.*")
                        .all();
                if (CollectionUtils.isNotEmpty(allLinks)) {
                    page.addTargetRequests(allLinks.subList(0, 1));
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
                    sb.append(pageId).append("\n").append(date).append("\n").append(name).append("\n").append(count).append("\n");
                    String today = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
                    if (today.equalsIgnoreCase(date)) {
                        MailUtil.sendMail("公租房今日房源信息", sb.toString());
                    }else {
                        MailUtil.sendMail( "暂无新房源信息", "无今日开放房源信息，请耐心等待2小时后再次获取");
                    }
                }else {
                    MailUtil.sendMail("暂无新房源信息", "最新的新闻非房源开发信息，请耐心等待2小时后再次获取");
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

    public static void start() {
        HttpClientDownloader downloader = null;
        try {
            downloader = getHttpClientDownloader();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Spider.create(new NewHouseProcessor())
                .addUrl("http://www.shcngz.com/pages/news_list.aspx?mid=29&pageid=1")
                .setDownloader(downloader)
                .thread(3).run();
    }

}
