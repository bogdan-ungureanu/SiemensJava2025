package com.siemens.internship;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


/**
 * This class is used to run all tests in one run.
 */
@Suite
@SelectClasses({
        ItemServiceTest.class,
        ItemControllerTest.class,
        InternshipApplicationTests.class
})
public class InternshipTestSuite {

}
