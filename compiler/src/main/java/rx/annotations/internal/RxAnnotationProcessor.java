package rx.annotations.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.squareup.javawriter.JavaWriter;
import rx.annotations.RxObservable;
import rx.annotations.SuperClass;
import rx.subjects.PublishSubject;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;

public class RxAnnotationProcessor extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<String>(Arrays.asList(RxObservable.class.getName(), SuperClass.class.getName()));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // A VERY raw draft! Suitable for one example only!

    Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(RxObservable.class);
    Multimap<TypeMirror, ExecutableElement> classMethods = ArrayListMultimap.create();
    Map<TypeMirror, String[]> notOverridenMethods = new HashMap<TypeMirror, String[]>();
    for (Element m : methods) {
      TypeElement clazz = (TypeElement) m.getEnclosingElement();
      SuperClass annotation = clazz.getAnnotation(SuperClass.class);
      final TypeMirror initialSuperClass;
      try {
        annotation.value();
        throw new AssertionError("does not work");
      } catch (MirroredTypeException e) {
        initialSuperClass = e.getTypeMirror();
      }
      classMethods.put(initialSuperClass, (ExecutableElement) m);
      if (annotation.methods() != null) {
        notOverridenMethods.put(initialSuperClass, annotation.methods());
      }
    }

    for (TypeMirror superClass : classMethods.keySet()) {
      String targetClassName = superClass.toString();
      String packageName = targetClassName.substring(0, targetClassName.lastIndexOf("."));
      String superClassName = "Rx_".concat(targetClassName.substring(packageName.length() + 1));
      Collection<ExecutableElement> targetMethods = classMethods.get(superClass);

      try {
        JavaFileObject dst = processingEnv.getFiler().createSourceFile(packageName + "." + superClassName, (TypeElement) targetMethods.iterator().next().getEnclosingElement());
        JavaWriter out = new JavaWriter(dst.openWriter());

        out.emitPackage(packageName);
        out.emitImports(targetClassName);
        out.emitImports(rx.Observable.class, PublishSubject.class);
        out.beginType(superClassName, "class", EnumSet.noneOf(Modifier.class), targetClassName);

        for (ExecutableElement m : targetMethods) {
          String name = m.getSimpleName().toString();
          writeObservableMethod(out, m.getReturnType().toString(), name, m.getModifiers());
        }

        String[] additionalMethods = notOverridenMethods.get(superClass);
        if (additionalMethods != null) {
          for (String name : additionalMethods) {
            // This might be tricky! :)
            writeObservableMethod(out, "void", name, EnumSet.of(PROTECTED));
          }
        }

        out.endType();

        out.close();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }

    return false;
  }

  private void writeObservableMethod(JavaWriter out, String returnType, String name, Set<Modifier> modifiers) throws IOException {
    out.emitField("PublishSubject<String>", name.concat("Subject"), EnumSet.of(PRIVATE), "PublishSubject.create()");
    out.beginMethod("Observable<String>", name.concat("Observable"), EnumSet.of(PROTECTED));
    out.emitStatement("return " + name + "Subject");
    out.endMethod();
    out.emitAnnotation(Override.class);
    out.beginMethod(returnType, name, modifiers);
    out.emitStatement("super." + name + "()");
    out.emitStatement(name + "Subject.onNext(" + JavaWriter.stringLiteral(name) + ")");
    out.endMethod();
  }

}
