package com.sem.linkmine;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sem.linkmine.repositories.LinkRepository;
import org.junit.jupiter.api.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureDataMongo
public class SlackControllerTests {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private MongoTemplate mongoTemplate;
    @MockBean
    private LinkRepository linkRepository;

    private ObjectMapper mapper = new ObjectMapper();

    private String mineCommandPayload = "{\n" +
            "  \"api_app_id\": \"A03QFFWRYNP\",\n" +
            "  \"channel_id\": \"C030N4T8QF6\",\n" +
            "  \"channel_name\": \"random\",\n" +
            "  \"command\": \"/mine\",\n" +
            "  \"is_enterprise_install\": false,\n" +
            "  \"response_url\": \"https://hooks.slack.com/commands/T030GTU40TX/3841697314244/bmfgQqY2AJNTjRi1lomD5pIl\",\n" +
            "  \"team_domain\": \"sem-mqv1193\",\n" +
            "  \"team_id\": \"T030GTU40TX\",\n" +
            "  \"text\": \"pants\",\n" +
            "  \"token\": \"<token>\",\n" +
            "  \"trigger_id\": \"3863010016272.3016946136949.be3381d7383a250c0725b7f39123f663\",\n" +
            "  \"user_id\": \"U030KR6D4LA\",\n" +
            "  \"user_name\": \"joshuadaleharris\"\n" +
            "}";

    @Test
    public void issueQuery() throws Exception {
        var slackPayload = mapper.writeValueAsString(mineCommandPayload);
        var slackRequest = MockMvcRequestBuilders
                .post("/slack/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(slackPayload);

        mvc.perform(slackRequest)
                .andExpect(status().isOk())
                .andExpect(content().string(equalTo("Greetings from the LinkMine!")));
    }
}