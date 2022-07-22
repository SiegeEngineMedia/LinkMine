package com.sem.linkmine;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sem.linkmine.repositories.LinkRepository;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
public class SlackControllerTests {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private MongoTemplate mongoTemplate;
    @MockBean
    private LinkRepository linkRepository;

    @Test
    public void issueQuery() throws Exception {
        var slackPayload = new JSONObject();
        // slackPayload;
        var slackRequest = MockMvcRequestBuilders
                .post("/slack/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(slackPayload.toString());

        mvc.perform(slackRequest).andExpect(status().isOk());
    }
}