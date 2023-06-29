package zju.cst.aces.utils;


import lombok.Data;

import java.util.List;

/**
 * @Author volunze
 * @Date 2023/6/26 10:25
 * @PackageName:org.example.pojo
 * @ClassName: ErrorMessage
 * @Description: include class name for unit test and error message
 * @Version 1.0
 */
@Data
public class TestMessage {
    private String unitTest;
    private List<String> errorMessage;
    private ErrorType errorType;

    public enum ErrorType {
        COMPILE_ERROR,
        RUNTIME_ERROR
    }
}
