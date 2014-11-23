package rx.annotations.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.squareup.javawriter.JavaWriter;
import rx.annotations.RxObervable;
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
import java.util.EnumSet;
import java.util.HashSet;
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
    return new HashSet<String>(Arrays.asList(RxObervable.class.getName(), SuperClass.class.getName()));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // A VERY raw draft! Suitable for one example only!

    Set<? extends Element> methods = roundEnv.getElementsAnnotatedWith(RxObervable.class);
    Multimap<TypeElement, ExecutableElement> classMethods = ArrayListMultimap.create();
    for (Element m : methods) {
      classMethods.put((TypeElement) m.getEnclosingElement(), (ExecutableElement) m);
    }

    for (TypeElement clazz : classMethods.keySet()) {
      TypeMirror initialSuperClass = null;
      try {
        clazz.getAnnotation(SuperClass.class).value();
        throw new AssertionError("does not work");
      } catch (MirroredTypeException e) {
        initialSuperClass = e.getTypeMirror();
      }
      String targetClassName = clazz.getQualifiedName().toString();
      String packageName = targetClassName.substring(0, targetClassName.lastIndexOf("."));
      String superClass = "Rx_".concat(targetClassName.substring(packageName.length() + 1));
      try {
        JavaFileObject dst = processingEnv.getFiler().createSourceFile(packageName + "." + superClass, clazz);
        JavaWriter out = new JavaWriter(dst.openWriter());

        out.emitPackage(packageName);
        out.emitImports(initialSuperClass.toString());
        out.emitImports(rx.Observable.class, PublishSubject.class);
        out.beginType(superClass, "class", EnumSet.noneOf(Modifier.class), initialSuperClass.toString());

        for (ExecutableElement m : classMethods.get(clazz)) {
          String name = m.getSimpleName().toString();
          out.emitField("PublishSubject<String>", name.concat("Subject"), EnumSet.of(PRIVATE), "PublishSubject.create()");
          out.beginMethod("Observable<String>", name.concat("Observable"), EnumSet.of(PROTECTED));
          out.emitStatement("return " + name + "Subject");
          out.endMethod();
          out.emitAnnotation(Override.class);
          out.beginMethod(m.getReturnType().toString(), name, m.getModifiers());
          out.emitStatement("super." + name + "()");
          out.emitStatement(name + "Subject.onNext(" + JavaWriter.stringLiteral(name) + ")");
          out.endMethod();
        }

        out.endType();

        out.close();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }

    return false;
  }



}
