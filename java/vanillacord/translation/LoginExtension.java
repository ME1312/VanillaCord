package vanillacord.translation;

import bridge.asm.HierarchicalWriter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import vanillacord.data.ClassData;
import vanillacord.data.FieldData;
import vanillacord.data.MethodData;
import vanillacord.packaging.Package;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static bridge.asm.Types.*;
import static org.objectweb.asm.Opcodes.*;
import static vanillacord.translation.Translations.*;

public final class LoginExtension {
    private LoginExtension() {}

    public static void translate(Package file, ZipOutputStream stream) throws IOException {
        HierarchicalWriter cv = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassData buffer = (ClassData) file.sources.receive.arguments[0].data();
        String type = Type.getInternalName(LoginExtension.class);
        MethodData connection = null;

        for (MethodData method : ((ClassData) file.sources.connection.type.data()).methods.values()) {
            if ((method.access & (ACC_PUBLIC | ACC_STATIC)) == ACC_PUBLIC && method.arguments.length == 1 && file.sources.send.owner.clazz.implemented(method.arguments[0])) {
                connection = method;
                break;
            }
        }

        if (file.sources.receive.owner.clazz.extended(file.types.loadClass("java/lang/Record"))) {
            ClassData send = null;
            ClassData namespace = file.sources.namespace = (ClassData) file.sources.send.arguments[0].data();
            for (FieldData field : file.sources.send.owner.fields.values()) {
                if ((field.access & (ACC_STATIC | ACC_FINAL)) == ACC_FINAL && !field.type.isPrimitive()) {
                    send = (ClassData) field.type.data();
                    break;
                }
            }

            String namespaced, inner = type + "$1";
            cv.visit(V1_8, VCT_CLASS & ~ACC_PUBLIC, inner, null, "java/lang/Object", new String[] {send.clazz.type.getInternalName()});
            cv.visitOuterClass(type, "send", "(Ljava/lang/Object;ILjava/lang/Object;Lio/netty/buffer/ByteBuf;)V");
            cv.visitInnerClass(inner, null, null, 0);
            cv.visitField(VCT_FIELD & ~ACC_STATIC, "ns", namespaced = namespace.clazz.type.getDescriptor(), null, null);
            cv.visitField(VCT_FIELD & ~ACC_STATIC, "data", "Lio/netty/buffer/ByteBuf;", null, null);

            MethodVisitor mv = cv.visitMethod(ACC_SYNTHETIC, "<init>", '(' + namespaced + "Lio/netty/buffer/ByteBuf;)V", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD,
                        inner,
                        "ns",
                        namespaced
                );
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitFieldInsn(PUTFIELD,
                        inner,
                        "data",
                        "Lio/netty/buffer/ByteBuf;"
                );
                mv.visitMethodInsn(INVOKESPECIAL,
                        "java/lang/Object",
                        "<init>",
                        "()V",
                        false
                );
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            for (MethodData method : send.methods.values()) {
                if ((method.access & ACC_ABSTRACT) != 0) {
                    if (method.arguments.length == 0 && method.returns.equals(namespace.clazz)) {
                        mv = cv.visitMethod(VCT_METHOD & ~ACC_STATIC, method.name, method.descriptor, null, null); {
                            mv.visitCode();
                            mv.visitLabel(new Label());
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD,
                                    inner,
                                    "ns",
                                    namespaced
                            );
                            mv.visitInsn(ARETURN);
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        }
                    } else if (method.arguments.length == 1 && method.arguments[0].equals(buffer.clazz) && method.returns.type.equals(VOID_TYPE)) {
                        mv = cv.visitMethod(VCT_METHOD & ~ACC_STATIC, method.name, method.descriptor, null, null); {
                            mv.visitCode();
                            mv.visitLabel(new Label());
                            mv.visitVarInsn(ALOAD, 1);
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD,
                                    inner,
                                    "data",
                                    "Lio/netty/buffer/ByteBuf;"
                            );
                            mv.visitMethodInsn(INVOKEVIRTUAL,
                                    "io/netty/buffer/ByteBuf",
                                    "copy",
                                    "()Lio/netty/buffer/ByteBuf;",
                                    false
                            );
                            mv.visitMethodInsn(INVOKEVIRTUAL,
                                    buffer.clazz.type.getInternalName(),
                                    "writeBytes",
                                    "(Lio/netty/buffer/ByteBuf;)Lio/netty/buffer/ByteBuf;",
                                    false
                            );
                            mv.visitInsn(POP);
                            mv.visitInsn(RETURN);
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        }
                    } else {
                        mv = cv.visitMethod(VCT_METHOD & ~ACC_STATIC, method.name, method.descriptor, null, null); {
                            mv.visitCode();
                            mv.visitLabel(new Label());
                            cast(mv, VOID_TYPE, method.returns.type);
                            mv.visitInsn(method.returns.type.getOpcode(IRETURN));
                            mv.visitMaxs(0, 0);
                            mv.visitEnd();
                        }
                    }
                }
            }

            cv.visitEnd();
            stream.putNextEntry(new ZipEntry(inner + ".class"));
            stream.write(cv.toByteArray());

            ClassData receive = null;
            for (FieldData field : file.sources.receive.owner.fields.values()) {
                if ((field.access & (ACC_STATIC | ACC_FINAL)) == ACC_FINAL && !field.type.isPrimitive()) {
                    receive = (ClassData) field.type.data();
                    break;
                }
            }

            cv = new HierarchicalWriter(file.types, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cv.visit(V1_8, VCT_CLASS, type, null, "java/lang/Object", new String[] {receive.clazz.type.getInternalName()});
            cv.visitInnerClass(inner, null, null, 0);
            cv.visitField(VCT_FIELD & ~ACC_STATIC, "data", "Lio/netty/buffer/ByteBuf;", null, null);

            mv = cv.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "<init>", "(Lio/netty/buffer/ByteBuf;)V", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL,
                        "java/lang/Object",
                        "<init>",
                        "()V",
                        false
                );
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD,
                        type,
                        "data",
                        "Lio/netty/buffer/ByteBuf;"
                );
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            for (MethodData method : receive.methods.values()) {
                if ((method.access & ACC_ABSTRACT) != 0) {
                    mv = cv.visitMethod(VCT_METHOD & ~ACC_STATIC, method.name, method.descriptor, null, null); {
                        mv.visitCode();
                        mv.visitLabel(new Label());
                        cast(mv, VOID_TYPE, method.returns.type);
                        mv.visitInsn(method.returns.type.getOpcode(IRETURN));
                        mv.visitMaxs(0, 0);
                        mv.visitEnd();
                    }
                }
            }

            mv = cv.visitMethod(VCT_METHOD, "send", "(Ljava/lang/Object;ILjava/lang/Object;Lio/netty/buffer/ByteBuf;)V", null, null); {
                mv.visitCode();
                mv.visitLabel(new Label());
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(CHECKCAST, connection.owner.clazz.type.getInternalName());
                mv.visitTypeInsn(NEW, file.sources.send.owner.clazz.type.getInternalName());
                mv.visitInsn(DUP);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitTypeInsn(NEW, inner);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitTypeInsn(CHECKCAST, namespace.clazz.type.getInternalName());
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKESPECIAL,
                        inner,
                        "<init>",
                        '(' + namespaced + "Lio/netty/buffer/ByteBuf;)V",
                        false
                );
                mv.visitMethodInsn(INVOKESPECIAL,
                        file.sources.send.owner.clazz.type.getInternalName(),
                        "<init>",
                        "(I" + send.clazz.type.getDescriptor() + ")V",
                        false
                );
                mv.visitMethodInsn(INVOKEVIRTUAL,
                        connection.owner.clazz.type.getInternalName(),
                        connection.name,
                        connection.descriptor,
                        false
                );
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            methods(file, cv, receive, type);
        } else { // 1.20 > version
            cv.visit(V1_8, VCT_CLASS, type, null, "java/lang/Object", null);

            MethodData constructor = null;
            for (MethodData method : file.sources.send.owner.methods.values()) {
                if (method.arguments.length == 3 && method.name.equals("<init>") && method.arguments[0].type.getSort() == INT_SORT && method.arguments[2].equals(buffer.clazz)) {
                    file.sources.namespace = (ClassData) method.arguments[1].data();
                    constructor = method;
                    break;
                }
            }

            if (constructor != null) {
                MethodVisitor mv = cv.visitMethod(VCT_METHOD, "send", "(Ljava/lang/Object;ILjava/lang/Object;Lio/netty/buffer/ByteBuf;)V", null, null); {
                    mv.visitCode();
                    mv.visitLabel(new Label());
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitTypeInsn(CHECKCAST, connection.owner.clazz.type.getInternalName());
                    mv.visitTypeInsn(NEW, constructor.owner.clazz.type.getInternalName());
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitTypeInsn(CHECKCAST, file.sources.namespace.clazz.type.getInternalName());
                    mv.visitTypeInsn(NEW, buffer.clazz.type.getInternalName());
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitMethodInsn(INVOKESPECIAL,
                            buffer.clazz.type.getInternalName(),
                            "<init>",
                            "(Lio/netty/buffer/ByteBuf;)V",
                            false
                    );
                    mv.visitMethodInsn(INVOKESPECIAL,
                            constructor.owner.clazz.type.getInternalName(),
                            constructor.name,
                            constructor.descriptor,
                            false
                    );
                    mv.visitMethodInsn(INVOKEVIRTUAL,
                            connection.owner.clazz.type.getInternalName(),
                            connection.name,
                            connection.descriptor,
                            false
                    );
                    mv.visitInsn(RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                methods(file, cv, buffer, null);
            } else {
                fields(file, cv, connection, buffer, type);
            }
        }

        cv.visitEnd();
        stream.putNextEntry(new ZipEntry(type + ".class"));
        stream.write(cv.toByteArray());
    }

    private static void methods(Package file, HierarchicalWriter cv, ClassData match, String extension) {
        MethodData id = null, data = null;
        for (MethodData method : file.sources.receive.owner.methods.values()) {
            if ((method.access & (ACC_PUBLIC | ACC_STATIC)) == ACC_PUBLIC && method.arguments.length == 0) {
                if (method.returns.type.getSort() == INT_SORT && !method.name.equals("hashCode")) {
                    id = method;
                } else if (method.returns.equals(match.clazz)) {
                    data = method;
                }
            }
        }

        MethodVisitor mv = cv.visitMethod(VCT_METHOD, "getTransactionID", "(Ljava/lang/Object;)I", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, file.sources.receive.owner.clazz.type.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    id.owner.clazz.type.getInternalName(),
                    id.name,
                    id.descriptor,
                    false
            );
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        mv = cv.visitMethod(VCT_METHOD, "getData", "(Ljava/lang/Object;)Lio/netty/buffer/ByteBuf;", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, file.sources.receive.owner.clazz.type.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    data.owner.clazz.type.getInternalName(),
                    data.name,
                    data.descriptor,
                    false
            );
            if (extension != null) {
                Label label = new Label();
                mv.visitInsn(DUP);
                mv.visitTypeInsn(INSTANCEOF, extension);
                mv.visitJumpInsn(IFEQ, label);
                mv.visitTypeInsn(CHECKCAST, extension);
                mv.visitFieldInsn(GETFIELD,
                        extension,
                        "data",
                        "Lio/netty/buffer/ByteBuf;"
                );
                mv.visitInsn(ARETURN);
                mv.visitLabel(label);
                mv.visitInsn(POP);
                push(mv, null);
            }
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    // 1.17 > version
    private static void fields(Package file, HierarchicalWriter cv, MethodData connection, ClassData buffer, String type) {
        String sid = null, sns = null, sdata = null;
        for (FieldData field : file.sources.send.owner.fields.values()) {
            if ((field.access & ACC_STATIC) == 0) {
                if (field.type.type.getSort() == INT_SORT) {
                    sid = field.name;
                } else if (field.type.equals(buffer.clazz)) {
                    sdata = field.name;
                } else if (!field.type.isPrimitive()) {
                    file.sources.namespace = (ClassData) field.type.data();
                    sns = field.name;
                }
            }
        }

        String rid = null, rdata = null;
        for (FieldData field : file.sources.receive.owner.fields.values()) {
            if ((field.access & ACC_STATIC) == 0) {
                if (field.type.type.getSort() == INT_SORT) {
                    rid = field.name;
                } else if (field.type.equals(buffer.clazz)) {
                    rdata = field.name;
                }
            }
        }

        cv.visitField(VCT_FIELD, "setID", "Ljava/lang/invoke/MethodHandle;", null, null);
        cv.visitField(VCT_FIELD, "setNamespace", "Ljava/lang/invoke/MethodHandle;", null, null);
        cv.visitField(VCT_FIELD, "setData", "Ljava/lang/invoke/MethodHandle;", null, null);
        cv.visitField(VCT_FIELD, "getID", "Ljava/lang/invoke/MethodHandle;", null, null);
        cv.visitField(VCT_FIELD, "getData", "Ljava/lang/invoke/MethodHandle;", null, null);

        MethodVisitor mv = cv.visitMethod(ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitMethodInsn(INVOKESTATIC,
                    "java/lang/invoke/MethodHandles",
                    "lookup",
                    "()Ljava/lang/invoke/MethodHandles$Lookup;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, file.sources.send.owner.clazz);
            push(mv, sid);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectSetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "setID",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitInsn(DUP);
            push(mv, file.sources.send.owner.clazz);
            push(mv, sns);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectSetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "setNamespace",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitInsn(DUP);
            push(mv, file.sources.send.owner.clazz);
            push(mv, sdata);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectSetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "setData",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitInsn(DUP);
            push(mv, file.sources.receive.owner.clazz);
            push(mv, rid);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectGetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "getID",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            push(mv, file.sources.receive.owner.clazz);
            push(mv, rdata);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getDeclaredField",
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;",
                    false
            );
            mv.visitInsn(DUP);
            push(mv, true);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/reflect/Field",
                    "setAccessible",
                    "(Z)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandles$Lookup",
                    "unreflectGetter",
                    "(Ljava/lang/reflect/Field;)Ljava/lang/invoke/MethodHandle;",
                    false
            );
            mv.visitFieldInsn(PUTSTATIC,
                    type,
                    "getData",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        mv = cv.visitMethod(VCT_METHOD, "send", "(Ljava/lang/Object;ILjava/lang/Object;Lio/netty/buffer/ByteBuf;)V", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, connection.owner.clazz.type.getInternalName());
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "setData",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "setNamespace",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "setID",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitTypeInsn(NEW, file.sources.send.owner.clazz.type.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,
                    file.sources.send.owner.clazz.type.getInternalName(),
                    "<init>",
                    "()V",
                    false
            );
            mv.visitInsn(DUP_X1);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.send.owner.clazz.type.getDescriptor() + "I)V",
                    false
            );
            mv.visitInsn(DUP_X1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, file.sources.namespace.clazz.type.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.send.owner.clazz.type.getDescriptor() + file.sources.namespace.clazz.type.getDescriptor() + ")V",
                    false
            );
            mv.visitInsn(DUP_X1);
            mv.visitTypeInsn(NEW, buffer.clazz.type.getInternalName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitMethodInsn(INVOKESPECIAL,
                    buffer.clazz.type.getInternalName(),
                    "<init>",
                    "(Lio/netty/buffer/ByteBuf;)V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.send.owner.clazz.type.getDescriptor() + buffer.clazz.type.getDescriptor() + ")V",
                    false
            );
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    connection.owner.clazz.type.getInternalName(),
                    connection.name,
                    connection.descriptor,
                    false
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        mv = cv.visitMethod(VCT_METHOD, "getTransactionID", "(Ljava/lang/Object;)I", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "getID",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, file.sources.receive.owner.clazz.type.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.receive.owner.clazz.type.getDescriptor() + ")I",
                    false
            );
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        mv = cv.visitMethod(VCT_METHOD, "getData", "(Ljava/lang/Object;)Lio/netty/buffer/ByteBuf;", null, null); {
            mv.visitCode();
            mv.visitLabel(new Label());
            mv.visitFieldInsn(GETSTATIC,
                    type,
                    "getData",
                    "Ljava/lang/invoke/MethodHandle;"
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(CHECKCAST, file.sources.receive.owner.clazz.type.getInternalName());
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "java/lang/invoke/MethodHandle",
                    "invokeExact",
                    '(' + file.sources.receive.owner.clazz.type.getDescriptor() + ')' + buffer.clazz.type.getDescriptor(),
                    false
            );
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
}
