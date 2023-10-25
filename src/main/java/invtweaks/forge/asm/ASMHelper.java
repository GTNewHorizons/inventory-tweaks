package invtweaks.forge.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMHelper implements Opcodes {

    /**
     * Generate a new method "boolean name()", returning a constant value
     *
     * @param clazz  Class to add method to
     * @param name   Name of method
     * @param retval Return value of method
     */
    public static void generateBooleanMethodConst(ClassNode clazz, String name, boolean retval) {
        MethodNode method = new MethodNode(ASM5, ACC_PUBLIC, name, "()Z", null, null);
        InsnList code = method.instructions;
        code.add(new InsnNode(retval ? ICONST_1 : ICONST_0));
        code.add(new InsnNode(IRETURN));
        method.visitMaxs(1, 1);
        clazz.methods.add(method);
    }

    /**
     * Generate a new method "int name()", returning a constant value
     *
     * @param clazz  Class to add method to
     * @param name   Name of method
     * @param retval Return value of method
     */
    public static void generateIntegerMethodConst(ClassNode clazz, String name, short retval) {
        MethodNode method = new MethodNode(ASM5, ACC_PUBLIC, name, "()I", null, null);
        InsnList code = method.instructions;
        // Probably doesn't make a huge difference, but use BIPUSH if the value is small enough.
        if (retval >= Byte.MIN_VALUE && retval <= Byte.MAX_VALUE) {
            code.add(new IntInsnNode(BIPUSH, retval));
        } else {
            code.add(new IntInsnNode(SIPUSH, retval));
        }
        code.add(new InsnNode(IRETURN));
        method.visitMaxs(1, 1);
        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form "T name() { return this.forward(); }
     *
     * @param clazz       Class to generate new method on
     * @param name        Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype     Return type of method
     */
    public static void generateSelfForwardingMethod(ClassNode clazz, String name, String forwardname, Type rettype) {
        MethodNode method = new MethodNode(ASM5, ACC_PUBLIC, name, "()" + rettype.getDescriptor(), null, null);
        populateSelfForwardingMethod(method, forwardname, rettype, Type.getObjectType(clazz.name));
        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form "T name() { return Class.forward(this); }
     *
     * @param clazz       Class to generate new method on
     * @param name        Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype     Return type of method
     */
    public static void generateForwardingToStaticMethod(ClassNode clazz, String name, String forwardname, Type rettype,
            Type fowardtype) {
        MethodNode method = new MethodNode(ASM5, ACC_PUBLIC, name, "()" + rettype.getDescriptor(), null, null);
        populateForwardingToStaticMethod(method, forwardname, rettype, Type.getObjectType(clazz.name), fowardtype);
        clazz.methods.add(method);
    }

    /**
     * Generate a forwarding method of the form "T name() { return Class.forward(this); }
     *
     * @param clazz       Class to generate new method on
     * @param name        Name of method to generate
     * @param forwardname Name of method to call
     * @param rettype     Return type of method
     * @param thistype    Type to treat 'this' as for overload searching purposes
     */
    public static void generateForwardingToStaticMethod(ClassNode clazz, String name, String forwardname, Type rettype,
            Type fowardtype, Type thistype) {
        MethodNode method = new MethodNode(ASM5, ACC_PUBLIC, name, "()" + rettype.getDescriptor(), null, null);
        populateForwardingToStaticMethod(method, forwardname, rettype, thistype, fowardtype);
        clazz.methods.add(method);
    }

    /**
     * Populate a forwarding method of the form "T name() { return Class.forward(this); }"
     *
     * @param method      Method to generate code for
     * @param forwardname Name of method to call
     * @param rettype     Return type of method
     * @param thistype    Type of object method is being generated on
     * @param forwardtype Type to forward method to
     */
    public static void populateForwardingToStaticMethod(MethodNode method, String forwardname, Type rettype,
            Type thistype, Type forwardtype) {
        InsnList code = method.instructions;
        code.add(new VarInsnNode(thistype.getOpcode(ILOAD), 0));
        code.add(
                new MethodInsnNode(
                        INVOKESTATIC,
                        forwardtype.getInternalName(),
                        forwardname,
                        Type.getMethodDescriptor(rettype, thistype),
                        false));
        code.add(new InsnNode(rettype.getOpcode(IRETURN)));
        method.visitMaxs(1, 1);
    }

    /**
     * Populate a forwarding method of the form "T name() { return this.forward(); }" This is also valid for methods of
     * the form "static T name(S object) { return object.forward() }"
     *
     * @param method      Method to generate code for
     * @param forwardname Name of method to call
     * @param rettype     Return type of method
     * @param thistype    Type of object method is being generated on
     */
    public static void populateSelfForwardingMethod(MethodNode method, String forwardname, Type rettype,
            Type thistype) {
        InsnList code = method.instructions;
        code.add(new VarInsnNode(thistype.getOpcode(ILOAD), 0));
        code.add(
                new MethodInsnNode(
                        INVOKEVIRTUAL,
                        thistype.getInternalName(),
                        forwardname,
                        "()" + rettype.getDescriptor(),
                        false));
        code.add(new InsnNode(rettype.getOpcode(IRETURN)));
        method.visitMaxs(1, 1);
    }
}
