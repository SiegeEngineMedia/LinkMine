package com.sem.linkmine;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sem.linkmine.controllers.SlackController;
import com.sem.linkmine.services.ConstantsService;
import com.sem.linkmine.services.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

// @Testcontainers
// @AutoConfigureMockMvc
// @AutoConfigureDataMongo
// @SpringBootTest
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = LinkMineApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
// @WebMvcTest
// @WebAppConfiguration
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SlackServiceTests {
    @Autowired
    private ConstantsService constants;
    // @MockBean
    // private MongoTemplate mongoTemplate;
    // @MockBean
    // private LinkRepository linkRepository;
    private String mineCommandPayload;

    @BeforeAll
    void setup() throws Exception {
        Map<String, String> queryPayload = new HashMap<String, String>();
        queryPayload.put("api_app_id", "A03QFFWRYNP");
        queryPayload.put("channel_id", "C030N4T8QF6");
        queryPayload.put("channel_name", "random");
        queryPayload.put("command", constants.COMMAND_MINE_QUERY);
        queryPayload.put("is_enterprise_install", "false");
        queryPayload.put("response_url", "https://hooks.slack.com/commands/T030GTU40TX/3841697314244/bmfgQqY2AJNTjRi1lomD5pIl");
        queryPayload.put("team_domain", "sem-mqv1193");
        queryPayload.put("team_id", "T030GTU40TX");
        queryPayload.put("text", "pants");
        queryPayload.put("token", "rjr0SjWNsVaaqTnmitFF14NY");
        queryPayload.put("trigger_id", "3863010016272.3016946136949.be3381d7383a250c0725b7f39123f663");
        queryPayload.put("user_id", "U030KR6D4LA");
        queryPayload.put("user_name", "joshuadaleharris");
        mineCommandPayload = StringUtils.getDataString(queryPayload);
    }

    @Test
    public void mineQuery() throws Exception {
    }
}