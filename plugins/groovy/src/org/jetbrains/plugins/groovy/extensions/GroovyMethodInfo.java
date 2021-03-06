package org.jetbrains.plugins.groovy.extensions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PairFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.refactoring.GroovyNamesUtil;
import org.jetbrains.plugins.groovy.util.ClassInstanceCache;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class GroovyMethodInfo {
  
  private static volatile Map<String, Map<String, List<GroovyMethodInfo>>> METHOD_INFOS;
  private static Map<String, Map<String, List<GroovyMethodInfo>>> LIGHT_METHOD_INFOS;

  private static final Set<String> myAllSupportedNamedArguments = new HashSet<String>();

  private final List<String> myParams;
  private final ClassLoader myClassLoader;

  private final String myReturnType;
  private final String myReturnTypeCalculatorClassName;
  private PairFunction<GrMethodCall, PsiMethod, PsiType> myReturnTypeCalculatorInstance;

  private final Map<String, NamedArgumentDescriptor> myNamedArguments;
  private final String myNamedArgProviderClassName;
  private GroovyNamedArgumentProvider myNamedArgProviderInstance;

  private Map<String, String> myNamedArgReferenceProviderClassNames;

  private static void ensureInit() {
    if (METHOD_INFOS != null) return;

    Map<String, Map<String, List<GroovyMethodInfo>>> methodInfos = new HashMap<String, Map<String, List<GroovyMethodInfo>>>();
    Map<String, Map<String, List<GroovyMethodInfo>>> lightMethodInfos = new HashMap<String, Map<String, List<GroovyMethodInfo>>>();

    for (GroovyClassDescriptor classDescriptor : GroovyClassDescriptor.EP_NAME.getExtensions()) {
      ClassLoader classLoader = classDescriptor.getLoaderForClass();
      for (GroovyMethodDescriptor method : classDescriptor.methods) {
        addMethodDescriptor(methodInfos, method, classLoader, classDescriptor.className);
      }
    }

    for (GroovyMethodDescriptorExtension methodDescriptor : GroovyMethodDescriptorExtension.EP_NAME.getExtensions()) {
      if (methodDescriptor.className != null) {
        assert methodDescriptor.lightMethodKey == null;
        addMethodDescriptor(methodInfos, methodDescriptor, methodDescriptor.getLoaderForClass(), methodDescriptor.className);
      }
      else {
        assert methodDescriptor.className == null;
        addMethodDescriptor(lightMethodInfos, methodDescriptor, methodDescriptor.getLoaderForClass(), methodDescriptor.lightMethodKey);
      }
    }

    processUnnamedDescriptors(lightMethodInfos);
    processUnnamedDescriptors(methodInfos);

    LIGHT_METHOD_INFOS = lightMethodInfos;
    METHOD_INFOS = methodInfos;
  }

  private static void processUnnamedDescriptors(Map<String, Map<String, List<GroovyMethodInfo>>> map) {
    for (Map<String, List<GroovyMethodInfo>> methodMap : map.values()) {
      List<GroovyMethodInfo> unnamedMethodDescriptors = methodMap.get(null);
      if (unnamedMethodDescriptors != null) {
        for (Map.Entry<String, List<GroovyMethodInfo>> entry : methodMap.entrySet()) {
          if (entry.getKey() != null) {
            entry.getValue().addAll(unnamedMethodDescriptors);
          }
        }
      }
    }
 }

  @Nullable
  private static List<GroovyMethodInfo> getInfos(Map<String, Map<String, List<GroovyMethodInfo>>> map, String key, PsiMethod method) {
    Map<String, List<GroovyMethodInfo>> methodMap = map.get(key);
    if (methodMap == null) return null;

    List<GroovyMethodInfo> res = methodMap.get(method.getName());
    if (res == null) {
      res = methodMap.get(null);
    }
    
    return res;
  }
  
  public static List<GroovyMethodInfo> getInfos(PsiMethod method) {
    ensureInit();

    List<GroovyMethodInfo> lightMethodInfos = null;

    Object methodKind = GrLightMethodBuilder.getMethodKind(method);
    if (methodKind instanceof String) {
      lightMethodInfos = getInfos(LIGHT_METHOD_INFOS, (String)methodKind, method);
    }
    
    List<GroovyMethodInfo> methodInfos = null;

    PsiClass containingClass = method.getContainingClass();
    if (containingClass != null) {
      methodInfos = getInfos(METHOD_INFOS, containingClass.getQualifiedName(), method);
    }
    
    if (methodInfos == null) {
      return lightMethodInfos == null ? Collections.<GroovyMethodInfo>emptyList() : lightMethodInfos;
    }
    else {
      if (lightMethodInfos == null) {
        return methodInfos;
      }
      else {
        return ContainerUtil.concat(lightMethodInfos, methodInfos);
      }
    }
  }

  public GroovyMethodInfo(GroovyMethodDescriptor method, @NotNull ClassLoader classLoader) {
    myClassLoader = classLoader;
    myParams = method.getParams();
    myReturnType = method.returnType;
    myReturnTypeCalculatorClassName = method.returnTypeCalculator;
    assert myReturnType == null || myReturnTypeCalculatorClassName == null;

    myNamedArguments = method.getArgumentsMap();
    myNamedArgProviderClassName = method.namedArgsProvider;

    myNamedArgReferenceProviderClassNames = method.getNamedArgumentsReferenceProviders();

    myAllSupportedNamedArguments.addAll(myNamedArgReferenceProviderClassNames.keySet());

    if (ApplicationManager.getApplication().isInternal()) {
      // Check classes to avoid typo.

      assertClassExists(myNamedArgProviderClassName, GroovyNamedArgumentProvider.class);

      assertClassExists(myReturnTypeCalculatorClassName, PairFunction.class);

      for (String className : myNamedArgReferenceProviderClassNames.values()) {
        assertClassExists(className, PsiReferenceProvider.class, GroovyNamedArgumentReferenceProvider.class);
      }
    }
  }

  private void assertClassExists(@Nullable String className, Class<?> ... types) {
    if (className == null) return;

    try {
      Class<?> aClass = myClassLoader.loadClass(className);
      for (Class<?> t : types) {
        if (t.isAssignableFrom(aClass)) return;
      }

      assert false : "Incorrect class type: " + aClass + " must be one of " + Arrays.asList(types);

      assert Modifier.isPublic(aClass.getConstructor(ArrayUtil.EMPTY_CLASS_ARRAY).getModifiers());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void addMethodDescriptor(Map<String, Map<String, List<GroovyMethodInfo>>> res,
                                          GroovyMethodDescriptor method,
                                          @NotNull ClassLoader classLoader,
                                          @NotNull String key) {
    if (method.methodName == null) {
      addMethodDescriptor(res, method, classLoader, null, key);
    }
    else {
      for (StringTokenizer st = new StringTokenizer(method.methodName, " \t,;"); st.hasMoreTokens(); ) {
        String name = st.nextToken();
        assert GroovyNamesUtil.isIdentifier(name);
        addMethodDescriptor(res, method, classLoader, name, key);
      }
    }
  }
  
  private static void addMethodDescriptor(Map<String, Map<String, List<GroovyMethodInfo>>> res,
                                          GroovyMethodDescriptor method,
                                          @NotNull ClassLoader classLoader,
                                          @Nullable String methodName,
                                          @NotNull String key) {
    Map<String, List<GroovyMethodInfo>> methodMap = res.get(key);
    if (methodMap == null) {
      methodMap = new HashMap<String, List<GroovyMethodInfo>>();
      res.put(key, methodMap);
    }

    List<GroovyMethodInfo> methodsList = methodMap.get(methodName);
    if (methodsList == null) {
      methodsList = new ArrayList<GroovyMethodInfo>();
      methodMap.put(methodName, methodsList);
    }

    methodsList.add(new GroovyMethodInfo(method, classLoader));
  }

  @Nullable
  public String getReturnType() {
    return myReturnType;
  }

  public boolean isReturnTypeCalculatorDefined() {
    return myReturnTypeCalculatorClassName != null;
  }

  @NotNull
  public PairFunction<GrMethodCall, PsiMethod, PsiType> getReturnTypeCalculator() {
    if (myReturnTypeCalculatorInstance == null) {
      myReturnTypeCalculatorInstance = ClassInstanceCache.getInstance(myReturnTypeCalculatorClassName, myClassLoader);
    }
    return myReturnTypeCalculatorInstance;
  }

  public static Set<String> getAllSupportedNamedArguments() {
    ensureInit();

    return myAllSupportedNamedArguments;
  }

  /**
   * @return instance of PsiReferenceProvider or GroovyNamedArgumentReferenceProvider or null.
   */
  @Nullable
  public Object getNamedArgReferenceProvider(String namedArgumentName) {
    String className = myNamedArgReferenceProviderClassNames.get(namedArgumentName);
    if (className == null) return null;

    return ClassInstanceCache.getInstance(className, myClassLoader);
  }

  @Nullable
  public Map<String, NamedArgumentDescriptor> getNamedArguments() {
    return myNamedArguments;
  }

  public boolean isNamedArgumentProviderDefined() {
    return myNamedArgProviderClassName != null;
  }

  public GroovyNamedArgumentProvider getNamedArgProvider() {
    if (myNamedArgProviderInstance == null) {
      myNamedArgProviderInstance = ClassInstanceCache.getInstance(myNamedArgProviderClassName, myClassLoader);
    }
    return myNamedArgProviderInstance;
  }

  public boolean isApplicable(@NotNull PsiMethod method) {
    if (myParams == null) {
      return true;
    }

    PsiParameterList parameterList = method.getParameterList();

    if (parameterList.getParametersCount() != myParams.size()) return false;

    PsiParameter[] parameters = parameterList.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (!TypesUtil.isClassType(parameters[i].getType(), myParams.get(i))) {
        return false;
      }
    }

    return true;
  }
}
