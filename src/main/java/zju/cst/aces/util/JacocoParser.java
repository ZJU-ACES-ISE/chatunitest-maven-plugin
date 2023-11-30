package zju.cst.aces.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.objectweb.asm.Type;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import zju.cst.aces.MethodCoverageMojo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JacocoParser {
    /**
     * @param xmlFilePath     jacoco.xml path
     * @param className       TargetClassName eg:"name.pehl.piriti.converter.client.CharacterConverter"
     * @param methodSignature eg:"convert(String)"
     * @return
     */
    public List<CoverageInfo> getCoverageInfo(String xmlFilePath, String className, String methodSignature) {
        //设置返回值为json类型
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode coverageArray = objectMapper.createArrayNode();
        //single methodName
        String methodName = methodSignature.split("\\(")[0];
        //jacoco需要的格式
        className=className.replaceAll("\\.","/");
        //遇到jacoco对signature解析（更改参数类型）的问题，将每一个method的coverageInfo存储，最后进行比较
        List<List<CoverageInfo>> coverageInfoList = new ArrayList<>();
        try {
            // 创建DOM解析器工厂
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); // 禁用DTD验证
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 解析XML文件
            Document document = builder.parse(new File(xmlFilePath));
            // 查找对应的方法信息
            NodeList packageNodes = document.getElementsByTagName("package");
            for (int i = 0; i < packageNodes.getLength(); i++) {
                Element packageElement = (Element) packageNodes.item(i);
                NodeList classNodes = packageElement.getElementsByTagName("class");
                for (int j = 0; j < classNodes.getLength(); j++) {
                    Element classElement = (Element) classNodes.item(j);
                    if (classElement.getAttribute("name").equals(className)) {
                        NodeList methodNodes = classElement.getElementsByTagName("method");
                        for (int k = 0; k < methodNodes.getLength(); k++) {
                            Element methodElement = (Element) methodNodes.item(k);
                            String methodNameAttr = methodElement.getAttribute("name");
                            String methodDescAttr = methodElement.getAttribute("desc");
                            //method signature完全匹配
                            if (methodName.equals(methodNameAttr) && (methodNameAttr + parseMethodDescriptor(methodDescAttr)).replaceAll(" ", "").equals(methodSignature.replaceAll(" ", ""))) {
                                return getCoverageInfo(methodElement);
                            }
                            //只比较方法名，不比较签名
                            else if (methodName.equals(methodNameAttr)) {
                                // 找到了对应的方法信息
                                coverageInfoList.add(getCoverageInfo(methodElement));
                            }
                        }
                    }
                }
            }
        } catch (
                Exception e) {
            e.printStackTrace();
        }
//        return result.equals("")?"Failure":result;
        //先初始化最大覆盖率为0
        double maxCoverage=0;
        List<CoverageInfo> maxCoverageNode = new ArrayList<>();
        for (List<CoverageInfo> coverageInfos : coverageInfoList) {
            double covered=coverageInfos.get(0).getCovered();
            double missed=coverageInfos.get(0).getMissed();
            double coverage=covered/(covered+missed);
            if(coverage>=maxCoverage){
                maxCoverageNode=coverageInfos;
            }
        }
        return maxCoverageNode;
    }

    public static String parseMethodDescriptor(String descriptor) {
        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argumentType = argumentTypes[i];
            String typeName = argumentType.getClassName();
            int lastDotIndex = typeName.lastIndexOf('.');
            if (lastDotIndex != -1) {
                typeName = typeName.substring(lastDotIndex + 1);
            }
            result.append(typeName);

            if (i < argumentTypes.length - 1) {
                result.append(", ");
            }
        }
        result.append(")");
        return result.toString();
    }


    //html的覆盖率信息解析xxx%的覆盖率
    public CoverageData getJacocoHtmlParsedInfo(File htmlFile,String methodSignature,String testClassName){
        String htmlContent = "";
        try {
            htmlContent = FileUtils.readFileToString(htmlFile, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<CoverageData> coverageDataList = new ArrayList<>();
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
        org.jsoup.nodes.Element coverageTable = doc.getElementById("coveragetable");
        CoverageData coverageData = new CoverageData();
        if (coverageTable != null) {
            Elements rows = coverageTable.select("tbody > tr");
            for (org.jsoup.nodes.Element row : rows) {
                org.jsoup.nodes.Element methodNameElement = row.selectFirst("td[id^='a']");
                String methodName = methodNameElement.text();
                org.jsoup.nodes.Element instructionCoverageElement = row.selectFirst("td.ctr2:nth-child(3)");
                org.jsoup.nodes.Element branchCoverageElement = row.selectFirst("td.ctr2:nth-child(5)");
                String instructionCoverage = instructionCoverageElement.text();
                String branchCoverage = branchCoverageElement.text();
                coverageData = new CoverageData(testClassName.replaceAll("/","."),methodSignature,branchCoverage,instructionCoverage,null);
                if (methodName.replace(" ", "").equals(methodSignature.replace
                        (" ", ""))) {//methodSignature完全匹配
                    return coverageData;
                }
                else if(methodName.split("\\(")[0].equals(methodSignature.split("\\(")[0])){
                    coverageDataList.add(coverageData);
                }
            }
        }
        else {
            return null;
        }
        double maxInstructionCoverage=0;
        CoverageData maxCoverageData=new CoverageData();
        for (CoverageData data : coverageDataList) {
            double instructionCoverage=Double.parseDouble(data.getInstructionCoverage().split("\\%")[0]);
            if(instructionCoverage>=instructionCoverage)
            {
                maxCoverageData=data;
            }
        }
        return maxCoverageData;
    }

    //xml的覆盖信息解析，type covered missed等
    public List<CoverageInfo> getCoverageInfo(Element methodElement) {
        ArrayList<CoverageInfo> coverageItem = new ArrayList<>();
        NodeList counterNodes = methodElement.getElementsByTagName("counter");
        ObjectMapper objectMapper = new ObjectMapper();
        for (int l = 0; l < counterNodes.getLength(); l++) {
            Element counterElement = (Element) counterNodes.item(l);
            String type = counterElement.getAttribute("type");
            String missed = counterElement.getAttribute("missed");
            String covered = counterElement.getAttribute("covered");
            ObjectNode coverageNode = objectMapper.createObjectNode();
            coverageNode.put("type", type);
            coverageNode.put("missed", Integer.parseInt(missed));
            coverageNode.put("covered", Integer.parseInt(covered));
            CoverageInfo coverageInfo = new CoverageInfo(type, Integer.parseInt(missed), Integer.parseInt(covered));
            coverageItem.add(coverageInfo);
        }
        return coverageItem;
    }

    public class CoverageInfo {
        private String type;
        private Integer missed;
        private Integer covered;

        public CoverageInfo(String type, Integer missed, Integer covered) {
            this.type = type;
            this.missed = missed;
            this.covered = covered;
        }

        public CoverageInfo() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getMissed() {
            return missed;
        }

        public void setMissed(Integer missed) {
            this.missed = missed;
        }

        public Integer getCovered() {
            return covered;
        }

        public void setCovered(Integer covered) {
            this.covered = covered;
        }

        @Override
        public String toString() {
            return "CoverageInfo{" +
                    "type='" + type + '\'' +
                    ", missed=" + missed +
                    ", covered=" + covered +
                    '}';
        }

    }
    public class CoverageData{
        private String testClassName;
        private String methodSignature;
        private String branchCoverage;
        private String instructionCoverage;
        private List<JacocoParser.CoverageInfo> coverageInfo;

        public CoverageData() {
        }

        public CoverageData(String testClassName, String methodSignature, String branchCoverage, String instructionCoverage, List<CoverageInfo> coverageInfo) {
            this.testClassName = testClassName;
            this.methodSignature = methodSignature;
            this.branchCoverage = branchCoverage;
            this.instructionCoverage = instructionCoverage;
            this.coverageInfo = coverageInfo;
        }

        @Override
        public String toString() {
            return "CoverageData{" +
                    "testClassName='" + testClassName + '\'' +
                    ", methodSignature='" + methodSignature + '\'' +
                    ", branchCoverage='" + branchCoverage + '\'' +
                    ", instructionCoverage='" + instructionCoverage + '\'' +
                    ", coverageInfo=" + coverageInfo +
                    '}';
        }

        public String getTestClassName() {
            return testClassName;
        }

        public void setTestClassName(String testClassName) {
            this.testClassName = testClassName;
        }

        public String getMethodSignature() {
            return methodSignature;
        }

        public void setMethodSignature(String methodSignature) {
            this.methodSignature = methodSignature;
        }

        public String getBranchCoverage() {
            return branchCoverage;
        }

        public void setBranchCoverage(String branchCoverage) {
            this.branchCoverage = branchCoverage;
        }

        public String getInstructionCoverage() {
            return instructionCoverage;
        }

        public void setInstructionCoverage(String instructionCoverage) {
            this.instructionCoverage = instructionCoverage;
        }

        public List<CoverageInfo> getCoverageInfo() {
            return coverageInfo;
        }

        public void setCoverageInfo(List<CoverageInfo> coverageInfo) {
            this.coverageInfo = coverageInfo;
        }
    }
    public static void main(String[] args) {
        // 指定XML文件路径
/*        String jacocoXmlPath = "C:\\Users\\86138\\Desktop\\test-jacoco-parser\\jacoco.xml";

        // 指定要查找的信息
        String className = "de/openknowledge/jaxrs/reactive/GenericsUtil";
        String methodName = "fromGenericType";
        String methodSignature = "fromGenericType(Class, Class)";*/
        /*String jacocoXmlPath = "C:\\Users\\86138\\Desktop\\test-jacoco-parser\\jacoco_correct.xml";
        String className = "name.pehl.piriti.converter.client.CharacterConverter".replaceAll("\\.", "/");
        String methodName = "convert";
        String methodSignature = "convert(String)";*/
        /*String jacocoXmlPath="C:\\Users\\86138\\Desktop\\test-jacoco-parser\\27674753\\jacoco.xml";
        String jacocoHtmlPath="C:\\Users\\86138\\Desktop\\test-jacoco-parser\\27674753\\SubClassSupportInstanceInitializer.html";
        String methodSignature="getClass(T)";
        String className="org.hibernate.search.genericjpa.factory.impl.SubClassSupportInstanceInitializer";*/
        String jacocoXmlPath="C:\\Users\\86138\\Desktop\\test-jacoco-parser\\41423327\\jacoco.xml";
        JacocoParser jacocoParser = new JacocoParser();
        String jacocoHtmlPath="C:\\Users\\86138\\Desktop\\test-jacoco-parser\\41423327\\jacoco.html";
        String className="org.unix4j.util.RelativePathBase";
        String methodSignature="getRelativePathFor(File)";
        List<CoverageInfo> coverageInfoList = jacocoParser.getCoverageInfo(jacocoXmlPath, className,  methodSignature);
        System.out.println(coverageInfoList);
        CoverageData coverageData = jacocoParser.getJacocoHtmlParsedInfo(new File(jacocoHtmlPath), methodSignature, "com.github.haiger.dqueue.client.DQueue_popAlways_4_1_Test");
        List<JacocoParser.CoverageInfo> coverageInfo = jacocoParser.getCoverageInfo(jacocoXmlPath, className, methodSignature);
        coverageData.setCoverageInfo(coverageInfo);
        System.out.println(coverageData);
    }
}
