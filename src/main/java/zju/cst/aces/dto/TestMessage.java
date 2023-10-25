package zju.cst.aces.dto;


import lombok.Data;

import java.util.List;

/**
 * @author volunze
 * @date 2023/6/26 10:25
 * @PackageName:org.example.pojo
 * @className: ErrorMessage
 * @description: include class name for unit test and error message
 * @version 1.0
 */
@Data
public class TestMessage {
    private ErrorType errorType;
    private List<String> errorMessage;

    public enum ErrorType {
        COMPILE_ERROR,
        RUNTIME_ERROR
    }
}
