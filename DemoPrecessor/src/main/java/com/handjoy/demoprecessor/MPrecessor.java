package com.handjoy.demoprecessor;

import com.google.auto.service.AutoService;
import com.handjoy.demoannotation.BindView;
import com.handjoy.demoannotation.OnClick;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class MPrecessor extends AbstractProcessor {
    private Filer filerUtils; //
    private Elements elementUtils; //
    private Messager messagerUtils; //
    private Map<String, String> options; //

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        filerUtils = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        messagerUtils = processingEnvironment.getMessager();
        options = processingEnvironment.getOptions();
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(BindView.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        Set<? extends Element> elementsAnnotatedWith = roundEnvironment.getElementsAnnotatedWith(BindView.class);

        MethodSpec inject = null;
        String fileName = null, filePkg = null;
        ClassName log = ClassName.get("android.util", "Log");
        ClassName uiThread = ClassName.get("android.support.annotation", "UiThread");
        for (Element element : elementsAnnotatedWith) {
            if (element.getKind() == ElementKind.FIELD) {
                messagerUtils.printMessage(Diagnostic.Kind.OTHER, "ready go");
                Name clzName = element.getSimpleName();
                Set<Modifier> modifiers = element.getModifiers();
                TypeMirror typeMirror = element.asType();
                ElementKind kind = element.getKind();
                List<? extends Element> enclosedElements = element.getEnclosedElements();
                Element enclosingElement = element.getEnclosingElement();

                String msg = String.format("SimpleName:%s,modifiers:%s,typeMirror:%s,kind:%s,enclosedElements:%s,enclosingElement:%s",
                        clzName, modifiers, typeMirror, kind, enclosedElements, enclosingElement);
                messagerUtils.printMessage(Diagnostic.Kind.NOTE, msg);
//
                PackageElement packageOf = elementUtils.getPackageOf(element);
                Name simpleName = packageOf.getSimpleName();
                Name qualifiedName = packageOf.getQualifiedName();
                String pckMsg = String.format("simpleName:%s,QualifiedName:%s", simpleName, qualifiedName);
                messagerUtils.printMessage(Diagnostic.Kind.NOTE, pckMsg);

                clzName = enclosingElement.getSimpleName();
                modifiers = enclosingElement.getModifiers();
                typeMirror = enclosingElement.asType();
                kind = enclosingElement.getKind();
                enclosedElements = enclosingElement.getEnclosedElements();

                String parentEle = String.format(
                        "SimpleName:%s,modifiers:%s,typeMirror:%s,kind:%s,enclosedElements:%s",
                        clzName, modifiers, typeMirror, kind, enclosedElements
                );

                BindView annotation = element.getAnnotation(BindView.class);
                ClassName targetClass = ClassName.get(elementUtils.getPackageOf(enclosingElement).toString(), enclosingElement.getSimpleName().toString());
                ParameterSpec targetParams = ParameterSpec.builder(targetClass, enclosingElement.getSimpleName().toString().toLowerCase())
                        .addModifiers(Modifier.FINAL).build();
                if (inject == null) {
                    inject = MethodSpec.methodBuilder("inject")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(void.class)
                            .addParameter(targetParams)
                            .addStatement("$T.e($S,$S)", log, "element", msg)
                            .addStatement("$T.e($S,$S)", log, "element pkg", pckMsg)
                            .addStatement("$T.e($S,$S)", log, "element parent", parentEle)
                            .addAnnotation(uiThread).build();
                }
                inject = inject.toBuilder().addStatement("$L.$L=$L.findViewById($L)",
                        enclosingElement.getSimpleName().toString().toLowerCase(),
                        element.getSimpleName().toString(),
                        enclosingElement.getSimpleName().toString().toLowerCase(),
                        annotation.value())
                        .build();
                if (fileName == null) {
                    fileName = enclosingElement.getSimpleName().toString() + "_ViewBinding";
                }
                if (filePkg == null) {
                    filePkg = qualifiedName.toString();
                }
            } else {
                messagerUtils.printMessage(Diagnostic.Kind.ERROR, "not support");
            }
        }


        Set<? extends Element> onclickElements = roundEnvironment.getElementsAnnotatedWith(OnClick.class);

        for (Element element : onclickElements) {
            if (element.getKind() == ElementKind.METHOD) {
                OnClick annotation = element.getAnnotation(OnClick.class);
                Element enclosingElement = element.getEnclosingElement();
                int[] ids=annotation.value();
                ClassName targetClass = ClassName.get(elementUtils.getPackageOf(enclosingElement).toString(), enclosingElement.getSimpleName().toString());
                ParameterSpec targetParams = ParameterSpec.builder(targetClass, enclosingElement.getSimpleName().toString().toLowerCase())
                        .addModifiers(Modifier.FINAL)
                        .build();
                ClassName view=ClassName.get("android.view","View");
                if (inject==null) {
                    inject=MethodSpec.methodBuilder("inject")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(void.class)
                            .addParameter(targetParams)
                            .addAnnotation(uiThread)
                            .build();
                }
                for (int id : ids) {
                    inject=inject.toBuilder()
                            .addStatement("$L.findViewById($L).setOnClickListener(new $T.OnClickListener() {\n" +
                                            "      @Override\n" +
                                            "      public void onClick(View v) {\n" +
                                            "        $L.$L(v);\n" +
                                            "      }\n" +
                                            "    })\n",
                                    enclosingElement.getSimpleName().toString().toLowerCase(),
                                    id,
                                    view,
                                    enclosingElement.getSimpleName().toString().toLowerCase(),
                                    element.getSimpleName().toString()
                                    )
                            .build();
                }

                PackageElement packageOf = elementUtils.getPackageOf(element);
                Name qualifiedName = packageOf.getQualifiedName();
                if (fileName == null) {
                    fileName = enclosingElement.getSimpleName().toString() + "_ViewBinding";
                }
                if (filePkg == null) {
                    filePkg = qualifiedName.toString();
                }
            }else {
                messagerUtils.printMessage(Diagnostic.Kind.ERROR, "not support");
            }
        }

        if (inject == null) {
            return false;
        }
        TypeSpec bindingType = TypeSpec.classBuilder(fileName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(inject)
                .build();
        JavaFile javaFile = JavaFile
                .builder(filePkg, bindingType)
                .build();
        try {
            javaFile.writeTo(filerUtils);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
