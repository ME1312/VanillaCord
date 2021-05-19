package uk.co.thinkofdeath.vanillacord;

import io.netty.buffer.ByteBuf;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class VelocityHelper extends HelperVisitor {
    private final Class<?> networkManager;
    private final Class<?> loginListener;
    private final Class<?> loginPacket;
    private final Class<?> serverQuery;
    private final Class<?> clientQuery;

    protected VelocityHelper(LinkedHashMap<String, byte[]> queue, ClassWriter classWriter, String networkManager, String loginListener, String loginPacket, String serverQuery, String clientQuery) throws ClassNotFoundException {
        super(queue, classWriter);
        this.networkManager = Class.forName(Type.getType(networkManager).getClassName());
        this.loginListener = getClass(loginListener);
        this.loginPacket = getClass(loginPacket);
        this.serverQuery = getClass(serverQuery);
        this.clientQuery = getClass(clientQuery);
    }

    private static void classSearch(Class<?> next, ArrayList<Class<?>> types) {
        types.add(next);

        if (next.getSuperclass() != null && !types.contains(next.getSuperclass())) types.add(next.getSuperclass());
        for (Class<?> c : next.getInterfaces()) if (!types.contains(c)) {
            classSearch(c, types);
        }
    }

    @Override
    protected void generate() {
     // values.put("VCTR-NetworkManager", Type.getType(networkManager));
        {
            ArrayList<Class<?>> types = new ArrayList<>();
            classSearch(serverQuery, types);

            for (Method m : networkManager.getMethods()) {
                if (m.getParameterCount() == 1) {
                    Class<?> param = m.getParameterTypes()[0];
                    boolean select = false;
                    for (Class<?> type : types) {
                        if (param == type) {
                            select = true;
                            break;
                        }
                    }
                    if (select) {
                        values.put("VCTR-Packet", Type.getType(param));
                        values.put("VCMR-NetworkManager-SendPacket", m.getName());
                        break;
                    }
                }
            }
        }

        values.put("VCTR-LoginListener", Type.getType(loginListener));
        {
            for (Method m : loginListener.getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == loginPacket) {
                    values.put("VCMR-LoginListener-HandleIntercepted", m.getName());
                    break;
                }
            }
        }

        values.put("VCTR-InterceptedPacket", Type.getType(loginPacket));
        values.put("VCTR-LoginRequestPacket", Type.getType(serverQuery));
        {
            Constructor<?> qConstruct = null;
            for (Constructor<?> c : serverQuery.getConstructors()) {
                if (c.getParameterCount() == 3) {
                    qConstruct = c;
                    break;
                }
            }

            values.put("VCCR-LoginRequestPacket-UseFields", (qConstruct == null)?1:0);
            if (qConstruct == null) {
                for (Field f : serverQuery.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) {
                        if (f.getType() == int.class) {
                            values.put("VCFR-LoginRequestPacket-TransactionID", f.getName());
                        } else if (ByteBuf.class.isAssignableFrom(f.getType())) {
                            values.put("VCTR-PacketData", Type.getType(f.getType()));
                            values.put("VCFR-LoginRequestPacket-Data", f.getName());
                        } else if (!f.getType().isPrimitive()) {
                            values.put("VCTR-NamespacedKey", Type.getType(f.getType()));
                            values.put("VCFR-LoginRequestPacket-Namespace", f.getName());
                        }
                    }
                }
            } else {
                values.put("VCTR-NamespacedKey", Type.getType(qConstruct.getParameterTypes()[1]));
                values.put("VCTR-PacketData", Type.getType(qConstruct.getParameterTypes()[2]));
            }
        }

        values.put("VCTR-LoginResponsePacket", Type.getType(clientQuery));
        {
            for (Field f : clientQuery.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    if (f.getType() == int.class) {
                        values.put("VCFR-LoginResponsePacket-TransactionID", f.getName());
                    } else if (ByteBuf.class.isAssignableFrom(f.getType())) {
                        values.put("VCFR-LoginResponsePacket-Data", f.getName());
                    }
                }
            }
        }
    }
}
