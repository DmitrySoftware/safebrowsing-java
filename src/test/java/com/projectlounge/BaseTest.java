package com.projectlounge;

import com.projectlounge.bean.AppProperties;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

/**
 * Created by main on 24.08.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BaseTest {

    @Inject
    protected AppProperties properties;

    protected static final String PATH = "src/test/resources/";
    protected static final String TEST_URL = "http://up.mykings.pw:8888/update.txt";
    protected static final String THREAT_LIST_TXT = "threatList.txt";


}
