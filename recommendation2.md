# Extractor Class Enhancement Recommendations

## Executive Summary

The `Extractor` class is the most critical component in your code generation pipeline. This document provides comprehensive recommendations to improve its **readability**, **maintainability**, and **testability** while preserving its functionality.

**Current Issues:**
- 426 lines with mixed responsibilities
- Singleton pattern makes unit testing difficult
- Deep method nesting and complex conditional logic
- Mutable cache state shared across processing rounds
- Limited error handling and validation
- Code duplication in type detection logic

---

## 1. Architectural Improvements

### 1.1 Replace Singleton with Dependency Injection

**Current Problem:**
```java
public static Extractor getInstance() {
    if (INSTANCE == null) {
        INSTANCE = new Extractor();
    }
    return INSTANCE;
}
```

**Issues:**
- Cannot inject mock implementations for testing
- Global mutable state makes tests non-isolated
- Cannot create multiple instances for parallel processing
- Context is set once and cannot be reset for test scenarios

**Recommended Solution:**
```java
// Make Extractor a regular class (remove singleton)
public class Extractor {
    private final ProcessingContext context;
    private final TypeInfoExtractor typeInfoExtractor;
    private final ClassInfoExtractor classInfoExtractor;
    private final ExtractionCache cache;

    // Constructor injection
    public Extractor(ProcessingContext context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.typeInfoExtractor = new TypeInfoExtractor(context);
        this.classInfoExtractor = new ClassInfoExtractor(context);
        this.cache = new ExtractionCache();
    }

    // For testing with custom cache
    Extractor(ProcessingContext context, ExtractionCache cache) {
        this.context = context;
        this.typeInfoExtractor = new TypeInfoExtractor(context);
        this.classInfoExtractor = new ClassInfoExtractor(context);
        this.cache = cache;
    }
}
```

**Benefits:**
- Easy to mock for unit tests
- Can create isolated instances per test
- Can inject fake ProcessingContext for testing
- Clearer lifecycle management

---

### 1.2 Separate Cache Management

**Current Problem:**
Cache logic is embedded in Extractor with no abstraction.

**Recommended Solution:**
Create a dedicated `ExtractionCache` class:

```java
public class ExtractionCache {
    private final Map<TypeMirror, List<AttachedElementInfo>> elementCache = new HashMap<>();

    public Optional<List<AttachedElementInfo>> get(TypeMirror type) {
        return Optional.ofNullable(elementCache.get(type));
    }

    public void put(TypeMirror type, List<AttachedElementInfo> elements) {
        elementCache.put(type, elements);
    }

    public boolean contains(TypeMirror type) {
        return elementCache.containsKey(type);
    }

    public void clear() {
        elementCache.clear();
    }

    public int size() {
        return elementCache.size();
    }

    // For debugging
    public Set<String> getCachedTypeNames() {
        return elementCache.keySet().stream()
            .map(TypeMirror::toString)
            .collect(Collectors.toSet());
    }
}
```

**Benefits:**
- Cache behavior can be tested independently
- Easy to add cache statistics/monitoring
- Can swap implementations (LRU cache, timed eviction, etc.)
- Clear API for cache operations

---

### 1.3 Extract Specialized Sub-Extractors

**Current Problem:**
Extractor has too many responsibilities:
- Type detection
- Parameter extraction
- Return value extraction
- Class/Record metadata extraction
- Name conversion

**Recommended Solution:**
Split into focused classes:

```java
// 1. TypeInfoExtractor - Type detection and TypeInfo creation
public class TypeInfoExtractor {
    private final ProcessingContext context;

    public TypeInfoExtractor(ProcessingContext context) {
        this.context = context;
    }

    public TypeInfo extractTypeInfo(Element element) { /* ... */ }
    public TypeInfo extractTypeInfo(DeclaredType type) { /* ... */ }
    public boolean isSimpleType(TypeMirror type) { /* ... */ }
    public boolean isOptional(TypeMirror type) { /* ... */ }
    public boolean isCollection(TypeMirror type) { /* ... */ }
    public TypeMirror extractWrappedType(TypeMirror type) { /* ... */ }
}

// 2. ParameterExtractor - Method parameter extraction
public class ParameterExtractor {
    private final TypeInfoExtractor typeInfoExtractor;
    private final ExtractionCache cache;

    public List<ElementInfo> extractParams(ExecutableElement method) { /* ... */ }
    public List<String> extractParamNames(List<ElementInfo> params) { /* ... */ }
}

// 3. ReturnExtractor - Return value extraction
public class ReturnExtractor {
    private final TypeInfoExtractor typeInfoExtractor;
    private final ExtractionCache cache;

    public List<ReturnElementInfo> extractReturn(ExecutableElement method) { /* ... */ }
}

// 4. ClassInfoExtractor - Class/Record metadata extraction
public class ClassInfoExtractor {
    private final TypeInfoExtractor typeInfoExtractor;
    private final ProcessingContext context;

    public List<AttachedElementInfo> extractClassInfo(Element classOrRecord) { /* ... */ }
    private void handleRecord(AttachedElementInfo element, List<ExecutableElement> methods) { /* ... */ }
    private void handleClass(AttachedElementInfo element, ...) { /* ... */ }
}

// 5. Main Extractor - Facade coordinating sub-extractors
public class Extractor {
    private final ParameterExtractor parameterExtractor;
    private final ReturnExtractor returnExtractor;
    private final ClassInfoExtractor classInfoExtractor;
    private final ExtractionCache cache;

    public Extractor(ProcessingContext context) {
        TypeInfoExtractor typeInfoExtractor = new TypeInfoExtractor(context);
        this.cache = new ExtractionCache();
        this.parameterExtractor = new ParameterExtractor(typeInfoExtractor, cache);
        this.returnExtractor = new ReturnExtractor(typeInfoExtractor, cache);
        this.classInfoExtractor = new ClassInfoExtractor(typeInfoExtractor, context);
    }

    // Delegate to sub-extractors
    public List<ElementInfo> extractParams(ExecutableElement method) {
        return parameterExtractor.extractParams(method);
    }

    public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
        return returnExtractor.extractReturn(method);
    }

    public List<AttachedElementInfo> extractClassInfo(Element classOrRecord) {
        return classInfoExtractor.extractClassInfo(classOrRecord);
    }
}
```

**Benefits:**
- Each class has single responsibility
- Easier to test individual extractors
- Easier to understand and modify
- Can reuse extractors in different contexts

---

## 2. Method-Level Improvements

### 2.1 Refactor `extractReturn()` Method

**Current Issues:**
- Lines 83-112: Complex nested conditionals
- Handles both single and multi-output scenarios in one method
- Difficult to follow the logic flow

**Recommended Solution:**

```java
public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();

    if (isVoidReturn(returnType)) {
        return Collections.emptyList();
    }

    Output[] outputs = method.getAnnotation(PlsqlCallable.class).outputs();

    if (outputs.length == 0) {
        throw new IllegalStateException(
            "Method has return type but no @Output annotation: " + method.getSimpleName()
        );
    }

    return outputs.length == 1
        ? extractSingleOutput(method, outputs[0])
        : extractMultipleOutputs(method, outputs);
}

private List<ReturnElementInfo> extractSingleOutput(
    ExecutableElement method,
    Output output
) {
    DeclaredType returnType = (DeclaredType) method.getReturnType();
    ReturnElementInfo returnInfo = createReturnElementInfo(returnType, output);
    return List.of(returnInfo);
}

private List<ReturnElementInfo> extractMultipleOutputs(
    ExecutableElement method,
    Output[] outputs
) {
    DeclaredType returnType = (DeclaredType) method.getReturnType();
    List<AttachedElementInfo> attachedElements = cache.get(returnType.asElement().asType())
        .orElseThrow(() -> new IllegalStateException(
            "Return type not in cache: " + returnType
        ));

    ElementInfo parentElementInfo = createParentElementInfo(returnType);

    return Arrays.stream(outputs)
        .map(output -> mapOutputToReturnElement(output, attachedElements, parentElementInfo))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
}

private Optional<ReturnElementInfo> mapOutputToReturnElement(
    Output output,
    List<AttachedElementInfo> attachedElements,
    ElementInfo parent
) {
    return attachedElements.stream()
        .filter(element -> element.getName().equals(output.field()))
        .findFirst()
        .map(element -> {
            ReturnElementInfo returnInfo = createReturnElementInfo(element, output);
            returnInfo.setParent(parent);
            return returnInfo;
        });
}

private boolean isVoidReturn(TypeMirror returnType) {
    return Tools.isVoid(returnType.toString());
}
```

**Benefits:**
- Clear separation between single and multi-output logic
- Each method has one responsibility
- Easier to test individual scenarios
- Better error messages with context

---

### 2.2 Simplify `handleOutput()` Overloads

**Current Issues:**
- Two overloaded methods with similar but not identical logic (lines 115-176)
- TODO comment indicates incomplete wrapper handling
- Code duplication in type checking and name setting

**Recommended Solution:**

```java
// Single unified method
private ReturnElementInfo createReturnElementInfo(
    TypeInfo typeInfo,
    String defaultName,
    Output output
) {
    String finalName = output.field().isBlank() ? defaultName : output.field();

    if (typeInfo.isSimple()) {
        return createSimpleReturnElement(typeInfo, finalName, output);
    }

    if (typeInfo.isWrapped()) {
        return createWrappedReturnElement(typeInfo, finalName, output);
    }

    return createComposedReturnElement(typeInfo, finalName, output);
}

private ReturnElementInfo createSimpleReturnElement(
    TypeInfo typeInfo,
    String name,
    Output output
) {
    JdbcHelper jdbcType = typeInfo.asJdbcHelper();
    if (jdbcType == null) {
        throw new IllegalStateException(
            "Type cannot be mapped to JDBC type: " + typeInfo.typeAsString()
        );
    }
    return new ReturnElementInfo(typeInfo, name, output, POS);
}

private ReturnElementInfo createWrappedReturnElement(
    TypeInfo typeInfo,
    String name,
    Output output
) {
    Element wrappedElement = typeInfo.getRawWrappedType();
    ComposedElementInfo composedInfo = convertInto(wrappedElement);
    composedInfo.setTypeInfo(typeInfo);
    composedInfo.setName(name);
    return new ReturnElementInfo(composedInfo, output, POS);
}

private ReturnElementInfo createComposedReturnElement(
    TypeInfo typeInfo,
    String name,
    Output output
) {
    Element element = typeInfo.getRawType();
    ComposedElementInfo composedInfo = convertInto(element, typeInfo);
    composedInfo.setName(name);
    return new ReturnElementInfo(composedInfo, output, POS);
}

// Adapter methods for different call sites
private ReturnElementInfo createReturnElementInfo(
    DeclaredType returnType,
    Output output
) {
    TypeInfo typeInfo = extractTypeInfo(returnType);
    return createReturnElementInfo(typeInfo, RETURN_NAME, output);
}

private ReturnElementInfo createReturnElementInfo(
    AttachedElementInfo element,
    Output output
) {
    return createReturnElementInfo(element.getTypeInfo(), element.getName(), output);
}
```

**Benefits:**
- Eliminates code duplication
- Single algorithm for all return element creation
- Easier to add new type categories
- Clear separation of type-specific logic

---

### 2.3 Improve `extractClassInfo()` Method

**Current Issues:**
- Lines 271-300: Does too many things
- Mixes cache checking, field extraction, and element processing
- No validation of extracted data before caching

**Recommended Solution:**

```java
public List<AttachedElementInfo> extractClassInfo(Element classOrRecord) {
    validateClassOrRecord(classOrRecord);

    return cache.get(classOrRecord.asType())
        .orElseGet(() -> extractAndCacheClassInfo(classOrRecord));
}

private void validateClassOrRecord(Element element) {
    if (element == null) {
        throw new IllegalArgumentException("Class or Record element cannot be null");
    }

    ElementKind kind = element.getKind();
    if (kind != ElementKind.CLASS && kind != ElementKind.RECORD) {
        throw new IllegalArgumentException(
            "Element must be a CLASS or RECORD, but was: " + kind
        );
    }
}

private List<AttachedElementInfo> extractAndCacheClassInfo(Element classOrRecord) {
    context.logDebug("Extracting class info for: " + classOrRecord.getSimpleName());

    ClassMetadata metadata = extractClassMetadata(classOrRecord);
    List<AttachedElementInfo> attachedElements =
        createAttachedElements(classOrRecord, metadata);

    validateAttachedElements(classOrRecord, attachedElements);

    cache.put(classOrRecord.asType(), attachedElements);
    context.logDebug("Cached " + attachedElements.size() + " elements");

    return attachedElements;
}

private ClassMetadata extractClassMetadata(Element classOrRecord) {
    List<Element> fields = extractFields(classOrRecord);
    List<ExecutableElement> methods = extractMethods(classOrRecord);
    List<ExecutableElement> constructors = extractConstructors(classOrRecord);

    return new ClassMetadata(
        classOrRecord.getKind(),
        fields,
        methods,
        constructors
    );
}

private List<AttachedElementInfo> createAttachedElements(
    Element classOrRecord,
    ClassMetadata metadata
) {
    return metadata.fields().stream()
        .map(field -> createAttachedElement(field, classOrRecord.getKind(), metadata))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
}

private Optional<AttachedElementInfo> createAttachedElement(
    Element field,
    ElementKind kind,
    ClassMetadata metadata
) {
    AttachedElementInfo element = new AttachedElementInfo();
    element.setName(extractNameAsStr(field));
    element.setTypeInfo(extractTypeInfo(field));

    if (kind == ElementKind.RECORD) {
        populateRecordAccessors(element, metadata.methods());
    } else {
        boolean success = populateClassAccessors(element, metadata);
        if (!success) {
            return Optional.empty(); // Skip inaccessible fields
        }
    }

    return Optional.of(element);
}

private void validateAttachedElements(Element classOrRecord, List<AttachedElementInfo> elements) {
    if (elements.isEmpty()) {
        context.logWarning(
            "No accessible fields found in: " + classOrRecord.getSimpleName()
        );
    }

    // Validate records have getters
    if (classOrRecord.getKind() == ElementKind.RECORD) {
        for (AttachedElementInfo element : elements) {
            if (element.getGetter() == null) {
                throw new IllegalStateException(
                    "Record field missing accessor: " + element.getName()
                );
            }
        }
    }
}

// Helper record for metadata
private record ClassMetadata(
    ElementKind kind,
    List<Element> fields,
    List<ExecutableElement> methods,
    List<ExecutableElement> constructors
) {}
```

**Benefits:**
- Clear separation of concerns
- Explicit validation with good error messages
- Cache logic separated from extraction logic
- Easier to test each step independently
- Metadata object makes parameter passing cleaner

---

### 2.4 Simplify Type Detection Logic

**Current Issues:**
- `isOptional()` and `isCollection()` use similar patterns (lines 67-82)
- Type checking logic duplicated in `TypeInfo.isSimple()` and `Extractor`
- String-based type checking is fragile

**Recommended Solution:**

```java
// In TypeInfoExtractor class
public class TypeInfoExtractor {
    private static final String OPTIONAL_TYPE = "java.util.Optional";
    private static final String COLLECTION_TYPE = "java.util.Collection";

    private static final Set<String> SIMPLE_TYPE_PREFIXES = Set.of(
        "java.lang.",
        "java.time.",
        "java.math."
    );

    private static final Set<String> SIMPLE_TYPES = Set.of(
        "java.util.Date"
    );

    private final TypeUtils typeUtils;
    private final Elements elementUtils;

    public TypeInfoExtractor(ProcessingContext context) {
        this.typeUtils = context.getProcessingEnv().getTypeUtils();
        this.elementUtils = context.getProcessingEnv().getElementUtils();
    }

    public boolean isOptional(TypeMirror type) {
        return isAssignableFrom(type, OPTIONAL_TYPE);
    }

    public boolean isCollection(TypeMirror type) {
        return isAssignableFrom(type, COLLECTION_TYPE);
    }

    private boolean isAssignableFrom(TypeMirror type, String baseTypeName) {
        TypeElement baseElement = elementUtils.getTypeElement(baseTypeName);
        if (baseElement == null) {
            return false;
        }
        TypeMirror baseType = baseElement.asType();
        TypeMirror erasedType = typeUtils.erasure(type);
        TypeMirror erasedBase = typeUtils.erasure(baseType);
        return typeUtils.isAssignable(erasedType, erasedBase);
    }

    public boolean isSimpleType(TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return true;
        }

        String typeName = type.toString();

        // Check exact matches
        if (SIMPLE_TYPES.contains(typeName)) {
            return true;
        }

        // Check prefixes
        return SIMPLE_TYPE_PREFIXES.stream()
            .anyMatch(typeName::startsWith);
    }

    public TypeCategory categorizeType(TypeMirror type) {
        if (isSimpleType(type)) {
            return TypeCategory.SIMPLE;
        }
        if (isOptional(type) || isCollection(type)) {
            return TypeCategory.WRAPPED;
        }
        return TypeCategory.COMPOSED;
    }
}

// Enum for clarity
public enum TypeCategory {
    SIMPLE,    // Primitives, String, Date, etc.
    WRAPPED,   // Optional<T>, Collection<T>
    COMPOSED   // Custom classes/records
}
```

**Benefits:**
- Constants eliminate magic strings
- `isAssignableFrom()` is more robust than string comparison
- `TypeCategory` enum makes code more explicit
- Centralized type checking logic
- Easy to add new type categories

---

## 3. Improve Error Handling and Validation

### 3.1 Add Comprehensive Input Validation

**Current Issues:**
- Limited null checks
- No validation of method signatures
- Silent failures in some cases

**Recommended Solution:**

```java
public class ExtractorValidator {

    public static void validateMethod(ExecutableElement method) {
        if (method == null) {
            throw new IllegalArgumentException("Method cannot be null");
        }

        if (method.getKind() != ElementKind.METHOD) {
            throw new IllegalArgumentException(
                "Element is not a method: " + method.getKind()
            );
        }
    }

    public static void validateCallableMethod(ExecutableElement method) {
        validateMethod(method);

        PlsqlCallable annotation = method.getAnnotation(PlsqlCallable.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                "Method missing @PlsqlCallable annotation: " + method.getSimpleName()
            );
        }

        validateOutputAnnotations(method, annotation);
    }

    private static void validateOutputAnnotations(
        ExecutableElement method,
        PlsqlCallable annotation
    ) {
        TypeMirror returnType = method.getReturnType();
        Output[] outputs = annotation.outputs();

        boolean hasReturn = !Tools.isVoid(returnType.toString());
        boolean hasOutputs = outputs.length > 0;

        if (hasReturn && !hasOutputs) {
            throw new IllegalStateException(
                "Method has return type but no @Output annotation: " +
                method.getSimpleName()
            );
        }

        if (!hasReturn && hasOutputs) {
            throw new IllegalStateException(
                "Method has @Output annotation but void return type: " +
                method.getSimpleName()
            );
        }

        // Function-specific validation
        if (annotation.type() == CallableType.FUNCTION) {
            if (outputs.length != 1) {
                throw new IllegalStateException(
                    "Function must have exactly one @Output, found: " +
                    outputs.length + " in " + method.getSimpleName()
                );
            }
            if (!hasReturn) {
                throw new IllegalStateException(
                    "Function cannot have void return type: " +
                    method.getSimpleName()
                );
            }
        }
    }

    public static void validateTypeInfo(TypeInfo typeInfo) {
        if (typeInfo == null) {
            throw new IllegalArgumentException("TypeInfo cannot be null");
        }

        if (typeInfo.getMirror() == null) {
            throw new IllegalStateException(
                "TypeInfo has null TypeMirror"
            );
        }

        if (typeInfo.getRawType() == null) {
            throw new IllegalStateException(
                "TypeInfo has null raw type"
            );
        }
    }
}

// Use in Extractor methods
public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
    ExtractorValidator.validateCallableMethod(method);
    // ... rest of implementation
}
```

**Benefits:**
- Early detection of invalid inputs
- Clear, actionable error messages
- Centralized validation logic
- Easier debugging

---

### 3.2 Improve Error Messages

**Current Issues:**
- Generic error messages lack context
- No guidance for fixing issues

**Recommended Solution:**

```java
public class ExtractionException extends RuntimeException {
    private final Element element;
    private final String resolution;

    public ExtractionException(String message, Element element, String resolution) {
        super(formatMessage(message, element, resolution));
        this.element = element;
        this.resolution = resolution;
    }

    private static String formatMessage(String message, Element element, String resolution) {
        StringBuilder sb = new StringBuilder();
        sb.append("Code generation error: ").append(message);

        if (element != null) {
            sb.append("\n  Element: ").append(element.getSimpleName());
            sb.append("\n  Type: ").append(element.getKind());
            sb.append("\n  Location: ").append(element.asType());
        }

        if (resolution != null) {
            sb.append("\n  Resolution: ").append(resolution);
        }

        return sb.toString();
    }
}

// Usage example
private ReturnElementInfo createSimpleReturnElement(...) {
    JdbcHelper jdbcType = typeInfo.asJdbcHelper();
    if (jdbcType == null) {
        throw new ExtractionException(
            "Cannot map type to JDBC type: " + typeInfo.typeAsString(),
            typeInfo.getRawType(),
            "Ensure the type is one of: String, Integer, Long, BigDecimal, Date, LocalDate, LocalDateTime, LocalTime, Boolean, Character"
        );
    }
    return new ReturnElementInfo(typeInfo, name, output, POS);
}
```

**Benefits:**
- Users know exactly what went wrong
- Users know how to fix the problem
- Easier debugging during development
- Better developer experience

---

## 4. Testing Improvements

### 4.1 Make Class Testable

**Recommended Test Structure:**

```java
public class ExtractorTest {
    private ProcessingContext mockContext;
    private ExtractionCache cache;
    private Extractor extractor;

    @BeforeEach
    void setUp() {
        mockContext = mock(ProcessingContext.class);
        ProcessingEnvironment mockEnv = mock(ProcessingEnvironment.class);
        when(mockContext.getProcessingEnv()).thenReturn(mockEnv);

        cache = new ExtractionCache();
        extractor = new Extractor(mockContext, cache);
    }

    @Test
    void extractReturn_withSimpleType_returnsSimpleReturnElement() {
        // Given: method returning String with @Output
        ExecutableElement method = createMockMethod(
            "getString",
            String.class,
            output("result")
        );

        // When
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTypeInfo().isSimple()).isTrue();
        assertThat(result.get(0).getName()).isEqualTo("result");
    }

    @Test
    void extractClassInfo_cachesBehavior() {
        // Given: a Record class
        Element recordElement = createMockRecord("Person",
            field("name", String.class),
            field("age", Integer.class)
        );

        // When: extract twice
        List<AttachedElementInfo> first = extractor.extractClassInfo(recordElement);
        List<AttachedElementInfo> second = extractor.extractClassInfo(recordElement);

        // Then: should return cached result
        assertThat(first).isSameAs(second);
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void extractReturn_withMultipleOutputs_mapsToFields() {
        // Given: method with multiple outputs
        Element returnClass = createMockClass("Result",
            field("id", Integer.class),
            field("name", String.class)
        );
        cache.put(returnClass.asType(), extractFieldInfo(returnClass));

        ExecutableElement method = createMockMethod(
            "getData",
            returnClass,
            output("id", "id_out"),
            output("name", "name_out")
        );

        // When
        List<ReturnElementInfo> result = extractor.extractReturn(method);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ReturnElementInfo::getName)
            .containsExactlyInAnyOrder("id", "name");
    }
}
```

**Benefits:**
- Can test without annotation processor infrastructure
- Fast, isolated unit tests
- Can test edge cases easily
- Clear test structure

---

### 4.2 Add Integration Test Support

```java
@SupportedAnnotationTypes("com.plsql.tools.annotations.*")
public class ExtractorIntegrationTest extends AbstractProcessor {

    @Test
    void testRealAnnotationProcessing() {
        // Use google/compile-testing library
        JavaFileObject testSource = JavaFileObjects.forSourceString(
            "test.TestPackage",
            """
            package test;
            import com.plsql.tools.annotations.*;

            @Package
            public abstract class TestPackage {
                @PlsqlCallable(outputs = @Output("result"))
                public abstract String getUser(Integer id);
            }
            """
        );

        Compilation compilation = Compiler.javac()
            .withProcessors(new PLSQLAnnotationProcessor())
            .compile(testSource);

        assertThat(compilation).succeeded();
        assertThat(compilation)
            .generatedSourceFile("test.TestPackageImpl")
            .hasSourceEquivalentTo(expectedGeneratedCode());
    }
}
```

---

## 5. Code Quality Improvements

### 5.1 Reduce Magic Strings and Numbers

**Current Issues:**
- "pos" string literal
- "__$" suffix for variable names
- Hard-coded type names

**Recommended Solution:**

```java
public class CodeGenConstants {
    // Variable naming
    public static final String VARIABLE_SUFFIX = "__$";
    public static final String POSITION_VAR = "pos";
    public static final String RESULT_SET_VAR = "rs";
    public static final String STATEMENT_VAR = "stmt";
    public static final String RETURN_VAR = "result";

    // Type names
    public static final String OPTIONAL_TYPE = "java.util.Optional";
    public static final String COLLECTION_TYPE = "java.util.Collection";
    public static final String LIST_TYPE = "java.util.List";
    public static final String RESULT_SET_TYPE = "java.sql.ResultSet";

    // Method prefixes
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";
    public static final String IS_PREFIX = "is";

    public static String variableName(String name) {
        return name + VARIABLE_SUFFIX;
    }
}

// Usage
String varName = CodeGenConstants.variableName(elementInfo.getName());
// Instead of: String.format("%s__$", elementInfo.getName());
```

---

### 5.2 Add Documentation

**Current Issues:**
- No JavaDoc comments
- Complex logic without explanation
- No usage examples

**Recommended Solution:**

```java
/**
 * Extracts metadata from Java elements to generate PL/SQL callable code.
 *
 * <p>This class is responsible for analyzing annotated methods and their
 * parameters/return types to extract the information needed for code generation.
 * Results are cached to avoid redundant processing.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ProcessingContext context = new ProcessingContext(processingEnv);
 * Extractor extractor = new Extractor(context);
 *
 * // Extract method parameters
 * List<ElementInfo> params = extractor.extractParams(method);
 *
 * // Extract return information
 * List<ReturnElementInfo> returnInfo = extractor.extractReturn(method);
 *
 * // Extract class/record metadata (cached)
 * List<AttachedElementInfo> classInfo = extractor.extractClassInfo(recordClass);
 * }</pre>
 *
 * <h2>Type Handling:</h2>
 * <ul>
 *   <li><b>Simple types:</b> Primitives, String, Date, BigDecimal - mapped directly to JDBC types</li>
 *   <li><b>Wrapped types:</b> Optional&lt;T&gt;, Collection&lt;T&gt; - unwrapped and processed recursively</li>
 *   <li><b>Composed types:</b> Custom classes/records - flattened into individual fields</li>
 * </ul>
 *
 * @see TypeInfo
 * @see ElementInfo
 * @see ExtractionCache
 * @since 0.0.1
 */
public class Extractor {

    /**
     * Extracts return value information from a method.
     *
     * <p>Handles three scenarios:
     * <ol>
     *   <li>Void return - returns empty list</li>
     *   <li>Single @Output - simple return value</li>
     *   <li>Multiple @Output - composite object with multiple fields</li>
     * </ol>
     *
     * @param method the method to analyze, must be annotated with @PlsqlCallable
     * @return list of return element information, empty if void return
     * @throws IllegalStateException if method has return type but no @Output annotation
     * @throws IllegalArgumentException if method is null
     * @see PlsqlCallable
     * @see Output
     */
    public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
        // implementation
    }
}
```

---

### 5.3 Use Optional Instead of Null

**Current Issues:**
- `extractReturn()` returns null for void (line 86)
- Null checks scattered throughout code

**Recommended Solution:**

```java
// Instead of returning null
public List<ReturnElementInfo> extractReturn(ExecutableElement method) {
    if (isVoidReturn(method.getReturnType())) {
        return Collections.emptyList();  // Not null
    }
    // ...
}

// Use Optional for cache
public Optional<List<AttachedElementInfo>> getCachedClassInfo(TypeMirror type) {
    return Optional.ofNullable(cache.get(type));
}

// Use Optional for method matching
private Optional<ExecutableElement> findGetter(String fieldName, List<ExecutableElement> methods) {
    return methods.stream()
        .filter(m -> isMatchingGetter(m, fieldName))
        .findFirst();
}
```

---

## 6. Performance Optimizations

### 6.1 Lazy Type Detection

**Current Issues:**
- TypeInfo eagerly extracts wrapped types even if not needed

**Recommended Solution:**

```java
public class TypeInfo {
    private TypeMirror mirror;
    private Element rawType;

    // Lazy initialization
    private TypeMirror wrappedType;
    private Element rawWrappedType;
    private Boolean isSimple;
    private Boolean isWrapped;

    public boolean isSimple() {
        if (isSimple == null) {
            isSimple = computeIsSimple();
        }
        return isSimple;
    }

    public boolean isWrapped() {
        if (isWrapped == null) {
            isWrapped = computeIsWrapped();
        }
        return isWrapped;
    }

    public TypeMirror getWrappedType() {
        if (wrappedType == null && isWrapped()) {
            wrappedType = extractWrappedType();
        }
        return wrappedType;
    }
}
```

---

### 6.2 Stream API Optimization

**Current Issues:**
- Multiple iterations over same collections
- Unnecessary intermediate collections

**Recommended Solution:**

```java
// Before (lines 375-378)
private List<Element> extractClassFields(Element record) {
    return record.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.FIELD)
            .collect(Collectors.toList());
}

// After - combine operations
private List<AttachedElementInfo> extractAttachedElements(Element record) {
    List<ExecutableElement> methods = extractMethods(record);

    return record.getEnclosedElements().stream()
        .filter(element -> element.getKind() == ElementKind.FIELD)
        .map(field -> createAttachedElement(field, methods))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
}
```

---

## 7. Implementation Roadmap

### Phase 1: Foundation (No Breaking Changes)
1. Add ExtractionCache class
2. Add ExtractorValidator class
3. Add CodeGenConstants class
4. Add JavaDoc to all public methods
5. Add unit tests for current implementation

**Estimated effort:** 2-3 days

### Phase 2: Refactoring (Internal Changes)
1. Extract TypeInfoExtractor
2. Refactor extractReturn() method
3. Simplify handleOutput() methods
4. Improve extractClassInfo()
5. Add comprehensive error messages

**Estimated effort:** 3-4 days

### Phase 3: Architecture (Breaking Changes)
1. Remove singleton pattern
2. Split into sub-extractors
3. Update all calling code
4. Migrate tests

**Estimated effort:** 4-5 days

### Phase 4: Polish
1. Add integration tests
2. Performance profiling
3. Documentation
4. Code review

**Estimated effort:** 2-3 days

**Total: 11-15 days**

---

## 8. Migration Example

### Before (Current):
```java
// In PLSQLAnnotationProcessor
Extractor extractor = Extractor.getInstance();
extractor.context(context);
List<ElementInfo> params = extractor.extractParams(method);
```

### After (Recommended):
```java
// In PLSQLAnnotationProcessor
Extractor extractor = new Extractor(context);
List<ElementInfo> params = extractor.extractParams(method);
```

The change is minimal for clients but enables much better testability.

---

## 9. Metrics for Success

After refactoring, you should see:

- **Test Coverage:**
  - Extractor: 80%+ line coverage
  - Critical methods: 95%+ coverage

- **Complexity:**
  - Average cyclomatic complexity: < 5 per method
  - Max cyclomatic complexity: < 10 per method
  - Max method length: < 50 lines

- **Maintainability:**
  - Each class < 300 lines
  - Each method < 30 lines
  - Clear single responsibility per class

- **Error Handling:**
  - All exceptions have context
  - All exceptions suggest resolution
  - No silent failures

---

## 10. Summary

The Extractor class is critical but needs improvement in three key areas:

1. **Testability:** Remove singleton, inject dependencies, split responsibilities
2. **Readability:** Simplify methods, reduce nesting, improve naming
3. **Maintainability:** Better error handling, validation, documentation

The recommended refactoring preserves all functionality while making the code:
- Easier to understand for new developers
- Easier to test in isolation
- Easier to extend with new features
- More resilient to errors with better validation

Start with Phase 1 (non-breaking changes) to get immediate benefits without disruption.
