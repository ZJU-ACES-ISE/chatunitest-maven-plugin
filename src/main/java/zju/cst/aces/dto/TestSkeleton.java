package zju.cst.aces.dto;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

import static zju.cst.aces.runner.AbstractRunner.repairImports;
import static zju.cst.aces.runner.AbstractRunner.repairPackage;
import static zju.cst.aces.runner.AbstractRunner.changeTestName;

@Data
public class TestSkeleton {
    private String testName;
    private String fullTestName;
    private String packageName;
    private List<String> imports;
    private String skeleton = "@ExtendWith(MockitoExtension.class)\npublic class TestClass {\n}";

    public static void main(String[] args) {
        TestSkeleton testSkeleton = new TestSkeleton(sk);
        String code = testSkeleton.build("@Test\npublic void testMethod(){}");
        System.out.println(code);
    }

    public TestSkeleton(String skeleton) {
        this.skeleton = skeleton;
        this.packageName = "";

        CompilationUnit cu = StaticJavaParser.parse(skeleton);
        cu.getPackageDeclaration().ifPresent(p -> this.packageName = p.getNameAsString());
        this.testName = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow().getNameAsString();
        this.fullTestName = packageName + "." + testName;
        this.imports = cu.getImports().stream().map(i -> i.toString().trim()).collect(Collectors.toList());
    }

    public TestSkeleton(PromptInfo promptInfo) {
        this.fullTestName = promptInfo.getFullTestName();
        this.testName = fullTestName.lastIndexOf(".") == -1 ? fullTestName : fullTestName.substring(fullTestName.lastIndexOf(".") + 1);
        this.packageName = promptInfo.classInfo.packageName;
        this.imports = promptInfo.classInfo.imports;
        this.skeleton = repairPackage(this.skeleton, this.packageName);
        this.skeleton = repairImports(this.skeleton, this.imports, true);
        this.skeleton = changeTestName(this.skeleton, this.testName);
    }

    public String build(String testMethod) {
        CompilationUnit cu = StaticJavaParser.parse(skeleton);
        MethodDeclaration tm = StaticJavaParser.parseMethodDeclaration(testMethod);
        cu.getClassByName(testName).ifPresent(c -> c.addMember(tm));
        return cu.toString();
    }
    private static String sk = "package com.selimhorri.app.service.impl;\n" +
            "import java.util.List;\n" +
            "import java.util.stream.Collectors;\n" +
            "import javax.transaction.Transactional;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "import org.springframework.web.client.RestTemplate;\n" +
            "import com.selimhorri.app.constant.AppConstant;\n" +
            "import com.selimhorri.app.domain.id.OrderItemId;\n" +
            "import com.selimhorri.app.dto.OrderDto;\n" +
            "import com.selimhorri.app.dto.OrderItemDto;\n" +
            "import com.selimhorri.app.dto.ProductDto;\n" +
            "import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;\n" +
            "import com.selimhorri.app.helper.OrderItemMappingHelper;\n" +
            "import com.selimhorri.app.repository.OrderItemRepository;\n" +
            "import com.selimhorri.app.service.OrderItemService;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "import lombok.extern.slf4j.Slf4j;\n" +
            "@Service\n" +
            "@Transactional\n" +
            "@Slf4j\n" +
            "@RequiredArgsConstructor\n" +
            "public class OrderItemServiceImpl {\n" +
            "}";
}
