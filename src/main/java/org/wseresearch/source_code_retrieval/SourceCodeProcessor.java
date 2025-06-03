package org.wseresearch.source_code_retrieval;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class SourceCodeProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;

        // Only generate MethodRegistry if it does not already exist
        boolean alreadyGenerated = false;
        try {
            processingEnv.getFiler().getResource(
                StandardLocation.SOURCE_OUTPUT,
                "org.wseresearch.source_code_retrieval",
                "MethodRegistry.java"
            );
            alreadyGenerated = true;
        } catch (IOException e) {
            // File does not exist yet, so we can generate it
        }
        if (alreadyGenerated) return false;

        try {
            List<String> methodEntries = new ArrayList<>();

            for (Element element : roundEnv.getRootElements()) {
                if (element.getKind() != ElementKind.CLASS) continue;

                TypeElement classElement = (TypeElement) element;
                String className = classElement.getSimpleName().toString();
                String packageName = processingEnv.getElementUtils().getPackageOf(classElement).toString();
                String fullPath = packageName.replace('.', '/') + "/" + className + ".java";

                FileObject file = processingEnv.getFiler().getResource(
                        StandardLocation.SOURCE_PATH, "", fullPath);

                String source = new String(file.openInputStream().readAllBytes(), StandardCharsets.UTF_8);

                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.METHOD) {
                        ExecutableElement method = (ExecutableElement) enclosed;
                        String methodName = method.getSimpleName().toString();

                        // Naive Method Source Extraction (can be replaced by regex or parser)
                        String methodSource = source.lines()
                                .filter(line -> line.contains(methodName + "("))
                                .findFirst().orElse("// could not find source");

                        methodEntries.add(
                                String.format("        new MethodInfo(\"%s\", \"%s\", \"%s\", \"%s\")",
                                        packageName, className, methodName,
                                        methodSource.replace("\"", "\\\""))
                        );
                    }
                }
            }

            String content = "package org.wseresearch.source_code_retrieval;\n\n" +
                    "import java.util.*;\n\n" +
                    "public class MethodRegistry {\n" +
                    "    public static final List<MethodInfo> METHODS = List.of(\n" +
                    String.join(",\n", methodEntries) +
                    "\n    );\n" +
                    "}";

            JavaFileObject generatedFile = processingEnv.getFiler()
                    .createSourceFile("org.wseresearch.source_code_retrieval.MethodRegistry.java");
            try (Writer writer = generatedFile.openWriter()) {
                writer.write(content);
            }

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
        }

        return false;
    }
    
}
