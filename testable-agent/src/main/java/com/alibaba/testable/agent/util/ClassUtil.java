package com.alibaba.testable.agent.util;

import com.alibaba.testable.agent.constant.ConstPool;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * @author flin
 */
public class ClassUtil {

    public static final byte TYPE_BYTE = 'B';
    public static final byte TYPE_CHAR = 'C';
    public static final byte TYPE_DOUBLE = 'D';
    public static final byte TYPE_FLOAT = 'F';
    public static final byte TYPE_INT = 'I';
    public static final byte TYPE_LONG = 'J';
    public static final byte TYPE_CLASS = 'L';
    public static final byte TYPE_SHORT = 'S';
    public static final byte TYPE_BOOL = 'Z';
    public static final byte TYPE_VOID = 'V';
    private static final byte PARAM_END = ')';
    private static final byte CLASS_END = ';';
    private static final byte TYPE_ARRAY = '[';

    public static final String CLASS_OBJECT = "java/lang/Object";
    private static final String CLASS_BYTE = "java/lang/Byte";
    private static final String CLASS_CHARACTER = "java/lang/Character";
    private static final String CLASS_DOUBLE = "java/lang/Double";
    private static final String CLASS_FLOAT = "java/lang/Float";
    private static final String CLASS_INTEGER = "java/lang/Integer";
    private static final String CLASS_LONG = "java/lang/Long";
    private static final String CLASS_SHORT = "java/lang/Short";
    private static final String CLASS_BOOLEAN = "java/lang/Boolean";
    private static final String EMPTY = "";
    private static final String METHOD_VALUE_OF = "valueOf";

    private static final Map<Byte, String> TYPE_MAPPING = new HashMap<Byte, String>();

    static {
        TYPE_MAPPING.put(TYPE_BYTE, CLASS_BYTE);
        TYPE_MAPPING.put(TYPE_CHAR, CLASS_CHARACTER);
        TYPE_MAPPING.put(TYPE_DOUBLE, CLASS_DOUBLE);
        TYPE_MAPPING.put(TYPE_FLOAT, CLASS_FLOAT);
        TYPE_MAPPING.put(TYPE_INT, CLASS_INTEGER);
        TYPE_MAPPING.put(TYPE_LONG, CLASS_LONG);
        TYPE_MAPPING.put(TYPE_SHORT, CLASS_SHORT);
        TYPE_MAPPING.put(TYPE_BOOL, CLASS_BOOLEAN);
        TYPE_MAPPING.put(TYPE_VOID, EMPTY);
    }

    /**
     * fit kotlin companion class name to original name
     * @param name a class name (which could be a companion class)
     * @return is companion class or not
     */
    public static boolean isCompanionClassName(String name) {
        return name.endsWith("$Companion");
    }

    /**
     * fit kotlin companion class name to original name
     * @param name a class name (which could be a companion class)
     * @return original name
     */
    public static String fitCompanionClassName(String name) {
        return name.replaceAll("\\$Companion$", "");
    }

    /**
     * get test class name from source class name
     * @param sourceClassName source class name
     * @return test class name
     */
    public static String getTestClassName(String sourceClassName) {
        return sourceClassName + ConstPool.TEST_POSTFIX;
    }

    /**
     * get source class name from test class name
     * @param testClassName test class name
     * @return source class name
     */
    public static String getSourceClassName(String testClassName) {
        return testClassName.substring(0, testClassName.length() - ConstPool.TEST_POSTFIX.length());
    }

    /**
     * parse method desc, fetch parameter types
     * @param desc method description
     * @return list of parameter types
     */
    public static List<Byte> getParameterTypes(String desc) {
        List<Byte> parameterTypes = new ArrayList<Byte>();
        boolean travelingClass = false;
        for (byte b : desc.getBytes()) {
            if (travelingClass) {
                if (b == CLASS_END) {
                    travelingClass = false;
                }
            } else {
                if (isPrimaryType(b)) {
                    parameterTypes.add(b);
                } else if (b == TYPE_CLASS) {
                    travelingClass = true;
                    parameterTypes.add(b);
                } else if (b == PARAM_END) {
                    break;
                }
            }
        }
        return parameterTypes;
    }

    /**
     * parse method desc, fetch return value types
     * @param desc method description
     * @return types of return value
     */
    public static String getReturnType(String desc) {
        int returnTypeEdge = desc.lastIndexOf(PARAM_END);
        char typeChar = desc.charAt(returnTypeEdge + 1);
        if (typeChar == TYPE_ARRAY) {
            return desc.substring(returnTypeEdge + 1);
        } else if (typeChar == TYPE_CLASS) {
            return desc.substring(returnTypeEdge + 2, desc.length() - 1);
        } else if (TYPE_MAPPING.containsKey((byte)typeChar)) {
            return TYPE_MAPPING.get((byte)typeChar);
        } else {
            return EMPTY;
        }
    }

    /**
     * Get method node to convert primary type to object type
     * @param type primary type to convert
     * @return converter method node
     */
    public static MethodInsnNode getPrimaryTypeConvertMethod(Byte type) {
        String objectType = TYPE_MAPPING.get(type);
        return (objectType == null) ? null :
            new MethodInsnNode(INVOKESTATIC, objectType, METHOD_VALUE_OF, toDescriptor(type, objectType), false);
    }

    /**
     * convert slash separated name to dot separated name
     * @param name original name
     * @return converted name
     */
    public static String toDotSeparatedName(String name) {
        return name.replace(ConstPool.SLASH, ConstPool.DOT);
    }

    /**
     * convert dot separated name to slash separated name
     * @param name original name
     * @return converted name
     */
    public static String toSlashSeparatedName(String name) {
        return name.replace(ConstPool.DOT, ConstPool.SLASH);
    }

    /**
     * convert dot separated name to byte code class name
     * @param className original name
     * @return converted name
     */
    public static String toByteCodeClassName(String className) {
        return (char)TYPE_CLASS + toSlashSeparatedName(className) + (char)CLASS_END;
    }

    /**
     * convert byte code class name to dot separated human readable name
     * @param className original name
     * @return converted name
     */
    public static String toDotSeparateFullClassName(String className) {
        return toDotSeparatedName(className).substring(1, className.length() - 1);
    }

    /**
     * convert byte code class name to slash separated human readable name
     * @param className original name
     * @return converted name
     */
    public static String toSlashSeparateFullClassName(String className) {
        return toSlashSeparatedName(className).substring(1, className.length() - 1);
    }

    private static String toDescriptor(Byte type, String objectType) {
        return "(" + (char)type.byteValue() + ")L" + objectType + ";";
    }

    private static boolean isPrimaryType(byte b) {
        return b == TYPE_BYTE || b == TYPE_CHAR || b == TYPE_DOUBLE || b == TYPE_FLOAT
            || b == TYPE_INT || b == TYPE_LONG || b == TYPE_SHORT || b == TYPE_BOOL;
    }
}
