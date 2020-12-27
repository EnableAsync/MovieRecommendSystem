package com.atguigu.business;

import com.atguigu.business.model.domain.Tag;
import com.atguigu.business.model.domain.User;
import com.atguigu.business.utils.Constant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.ApplicationContext;

/**
 * Created by wuyufei on 08/08/2017.
 */
public class Application {


    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");

        Settings settings = Settings.builder().put("cluster.name","es-cluster").build();
        TransportClient esClient = new PreBuiltTransportClient(settings);
        esClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

        GetResponse getResponse = esClient.prepareGet(Constant.ES_INDEX,Constant.ES_MOVIE_TYPE,"3062").get();

        Map<String,GetField> filed = getResponse.getFields();

        Object value = getResponse.getSourceAsMap().get("tags");

        if(value == null){
            UpdateRequest updateRequest = new UpdateRequest(Constant.ES_INDEX,Constant.ES_MOVIE_TYPE,"3062");
            updateRequest.doc(XContentFactory.jsonBuilder().startObject()
            .field("tags","abc")
            .endObject());
            esClient.update(updateRequest).get();
        }else{
            UpdateRequest updateRequest = new UpdateRequest(Constant.ES_INDEX,Constant.ES_MOVIE_TYPE,"2542");
            updateRequest.doc(XContentFactory.jsonBuilder().startObject()
                    .field("tags",value+"|abc")
            .endObject());
            esClient.update(updateRequest).get();
        }

        System.out.println(Math.round(4.466D));

    }

}
