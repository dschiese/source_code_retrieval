package org.wseresearch.source_code_retrieval;

public class MethodInfo {
    private final String packageName;
    private final String className;
    private final String methodName;
    private final String sourceCode;

    public MethodInfo(String packageName, String className, String methodName, String sourceCode) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.sourceCode = sourceCode;
    }

    public String getPackageName() { return packageName; }
    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public String getSourceCode() { return sourceCode; }
}