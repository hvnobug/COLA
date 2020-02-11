package com.alibaba.cola.mock.autotest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.cola.mock.ColaMockito;
import com.alibaba.cola.mock.utils.CommonUtils;
import com.alibaba.cola.mock.utils.Constants;
import com.alibaba.cola.mock.utils.FileUtils;

import org.apache.commons.lang3.StringUtils;

import static com.alibaba.cola.mock.utils.Constants.DOT;
import static com.alibaba.cola.mock.utils.Constants.NOTE_SYMBOL;

/**
 * @author shawnzhan.zxy
 * @date 2019/01/12
 */
public class TestClass {
    private static Map<Object, String> DEFAULT_PARAM_VALUE = new HashMap<>();
    static{
        DEFAULT_PARAM_VALUE.put("void", "");
        DEFAULT_PARAM_VALUE.put("char", "''");
        DEFAULT_PARAM_VALUE.put("short", "0");
        DEFAULT_PARAM_VALUE.put("int", "0");
        DEFAULT_PARAM_VALUE.put("long", "0L");
        DEFAULT_PARAM_VALUE.put("float", "0");
        DEFAULT_PARAM_VALUE.put("boolean", "false");
        DEFAULT_PARAM_VALUE.put("Boolean", "null");
        DEFAULT_PARAM_VALUE.put("Float", "null");
        DEFAULT_PARAM_VALUE.put("Long", "null");
        DEFAULT_PARAM_VALUE.put("Integer", "null");
        DEFAULT_PARAM_VALUE.put("Short", "null");
        DEFAULT_PARAM_VALUE.put("Char", "null");
        DEFAULT_PARAM_VALUE.put("String", "null");
    }

    private static final String NULL = "null";
    private static final String AUTO_DATA = "@";
    private static final String TESTFILE_SUFFIX = "Test.java";
    Class superClazz = null;
    Class testClazz;
    Method method;
    Object[] params;
    Map<String, Object> autoGenerateDataParameter = new HashMap<>();

    public TestClass(String testMethod, String superClazz, Object[] params){
        this.params = params;
        if(this.params == null){
            this.params = new Object[0];
        }
        initTestClazz(testMethod);
        initSuperClazz(superClazz);
    }

    public String getSimpleClassName(){
        return this.testClazz.getSimpleName();
    }

    /**
     * ${testClass}$${testMethod}Test
     * 实际测试类名
     * @return
     */
    public String getUnitTestClassName(){
        return this.testClazz.getSimpleName() + Constants.UNDERLINE + getMethodName() + "Test";
    }

    public String getClassName(){
        return this.testClazz.getName();
    }

    public String getMethodName(){
        return this.method.getName();
    }

    public String getSuperClazzName(){
        if(superClazz == null){
            return StringUtils.EMPTY;
        }
        return this.superClazz.getSimpleName();
    }

    public String getNamespace(){
        return this.testClazz.getPackage().getName();
    }

    public Set<String> buildImports(){
        Set<String> imports = new HashSet<>();
        if(superClazz != null) {
            imports.add(this.superClazz.getName());
        }
        imports.add(this.testClazz.getName());

        Class[] paramterClassLst = method.getParameterTypes();
        for(Class cls : paramterClassLst){
            if(null != DEFAULT_PARAM_VALUE.get(cls.getSimpleName())){
                continue;
            }
            imports.add(cls.getName());
        }

        Class returnType = method.getReturnType();
        if(null == DEFAULT_PARAM_VALUE.get(returnType.getSimpleName())){
            imports.add(returnType.getName());
        }

        return imports;
    }

    public List<String> buildParams(){
        List<String> ps = new ArrayList<>();
        Class[] paramterClassLst = method.getParameterTypes();
        for(int i=0; i< paramterClassLst.length; i++){
            Object param = getParam(i, paramterClassLst[i]);
            if(null != param  && param instanceof String){
                ps.add(param.toString());
            }else{
                String paramterName = getParamterName(i, paramterClassLst[i]);
                ps.add(paramterName);
            }
        }
        return ps;
    }

    public String buildReturn(){
        Class returnType = method.getReturnType();
        if(returnType == null || returnType.getName().equals(Constants.RETURN_TYPE_VOID)){
            return StringUtils.EMPTY;
        }
        return returnType.getSimpleName() + " result";
    }

    public List<String> buildVarDefinition(){
        List<String> vars = new ArrayList<>();
        Class[] paramterClassLst = method.getParameterTypes();
        for(int i=0; i< paramterClassLst.length; i++){
            Object paramValue  = getParam(i, paramterClassLst[i]);
            if(null != paramValue && paramValue instanceof String){
                continue;
            }
            String paramterName = getParamterName(i, paramterClassLst[i]);
            if(i < params.length && isAutoGenerateData(params[i])){
                vars.add(paramterClassLst[i].getSimpleName() + " " + paramterName + " = ColaMockito.getDataMap(\"" + paramterName + "\")");
                Object value = ColaMockito.pojo().manufacturePojo(paramterClassLst[i]);
                autoGenerateDataParameter.put(paramterName, value);
            }else if(paramValue != null){
                vars.add(paramterClassLst[i].getSimpleName() + " " + paramterName + " = ColaMockito.getDataMap(\"" + paramterName + "\")");
                autoGenerateDataParameter.put(paramterName, paramValue);
            }else {
                vars.add(paramterClassLst[i].getSimpleName() + " " + paramterName + " = null");
            }
        }
        return vars;
    }

    public String getFilePath(){
        String parentPath = FileUtils.appendSlashToURILast(FileUtils.getDefaultDirectory4TestFile());
        String filePath = parentPath + FileUtils.convertPackage2Path(getNamespace()) + getSimpleClassName() + Constants.UNDERLINE + getMethodName() + TESTFILE_SUFFIX;
        return filePath;
    }

    public Map<String, Object> getAutoGenerateDataParameter() {
        return autoGenerateDataParameter;
    }

    private Object getParam(int i, Class type){
        Object p = null;
        if(params.length > i){
            p = params[i];
        }
        if(isAutoGenerateData(p)){
            return null;
        }
        String param = DEFAULT_PARAM_VALUE.get(type.getSimpleName());
        if(p == null || p.toString().toLowerCase().equals(NULL)){
            if(param != null){
                return param;
            }
            return null;
        }
        if(null != param && StringUtils.isBlank(p.toString())){
            return param;
        }
        return convert2Param(type, p);
    }

    private Object convert2Param(Class type, Object paramValue){
        if(type == String.class){
            return paramValue.toString().endsWith("\"") ? paramValue.toString() : "\"" + paramValue + "\"";
        }else if(type == Long.class || type.getSimpleName().equals("long")){
            return paramValue.toString().toLowerCase().endsWith("l") ? paramValue.toString() : paramValue + "L";
        }else if(type == Float.class || type.getSimpleName().equals("float")){
            return paramValue.toString().toLowerCase().endsWith("f") ? paramValue.toString() : paramValue + "f";
        }
        return paramValue;
    }

    private boolean isAutoGenerateData(Object value){
        if(value != null && value.toString().toLowerCase().equals(AUTO_DATA)){
            return true;
        }
        return false;
    }

    private void initTestClazz(String testMethod){
        String[] sp = new String[2];
        if(testMethod.indexOf(NOTE_SYMBOL) > 0){
            sp = testMethod.split(NOTE_SYMBOL);
        }else{
            sp[0] = testMethod.substring(0, testMethod.lastIndexOf(DOT));
            sp[1] = testMethod.substring(testMethod.lastIndexOf(DOT) + 1, testMethod.length());
        }
        try {
            this.testClazz = Class.forName(sp[0]);
            this.method = getMethod(sp[1]);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initSuperClazz(String superClazz){
        if(StringUtils.isBlank(superClazz)){
            return;
        }
        try {
            this.superClazz = Class.forName(superClazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Method getMethod(String methodName){
        List<Class> paramClazzLst =  new ArrayList<>();
        for(Object o : params){
            paramClazzLst.add(o.getClass());
        }

        try {
            this.method = testClazz.getMethod(methodName, paramClazzLst.toArray(new Class[0]));
        } catch (NoSuchMethodException e) {
        }
        if(method != null){
            return method;
        }

        List<Method> methods = new ArrayList<>();
        methods.addAll(Arrays.asList(testClazz.getMethods()));
        Collections.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getModifiers() - o2.getModifiers();
            }
        });

        for(Method method : methods){
            if(method.getName().equals(methodName)){
                return method;
            }
        }
        return null;
    }

    private boolean isBasicType(Class clazz){
        String clazzName = clazz.toString();
        if(Long.class == clazz || Double.class == clazz || Float.class == clazz
            || Boolean.class == clazz){
            return true;
        }else if(clazzName.equals("int") || clazzName.equals("long") || clazzName.equals("boolean") || clazzName.equals("float")){
            return true;
        }
        return false;
    }

    private String getParamterName(int index, Class clazz){
        String varName;
        if(isBasicType(clazz)){
            varName = "p" + clazz.getSimpleName();
        }else{
            varName = CommonUtils.toLowerCaseFirstOne(clazz.getSimpleName());
        }
        return varName + index;
    }

}