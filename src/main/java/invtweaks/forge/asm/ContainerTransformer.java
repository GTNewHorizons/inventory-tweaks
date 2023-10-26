package invtweaks.forge.asm;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import invtweaks.forge.asm.compatibility.CompatibilityConfigLoader;
import invtweaks.forge.asm.compatibility.ContainerInfo;
import invtweaks.forge.asm.compatibility.MethodInfo;

public class ContainerTransformer implements IClassTransformer {

    private static final String IINV_TWEAKS_CONTAINER_INTERFACE = "invtweaks/forge/asm/interfaces/IInvTweaksContainer";
    private static final String VALID_INVENTORY_METHOD = "invtweaks$validInventory";
    private static final String VALID_CHEST_METHOD = "invtweaks$validChest";
    private static final String LARGE_CHEST_METHOD = "invtweaks$largeChest";
    private static final String SHOW_BUTTONS_METHOD = "invtweaks$showButtons";
    private static final String ROW_SIZE_METHOD = "invtweaks$rowSize";
    private static final String SLOT_MAP_METHOD = "invtweaks$slotMap";
    private static final String CONTAINER_CLASS_INTERNAL = "net/minecraft/inventory/Container";
    private static final String SLOT_MAPS_VANILLA_CLASS = "invtweaks/containers/VanillaSlotMaps";
    private static final String SLOT_MAPS_MODCOMPAT_CLASS = "invtweaks/containers/CompatibilitySlotMaps";
    private static final String ANNOTATION_CHEST_CONTAINER = "Linvtweaks/api/container/ChestContainer;";
    private static final String ANNOTATION_CHEST_CONTAINER_ROW_CALLBACK = "Linvtweaks/api/container/ChestContainer$RowSizeCallback;";
    private static final String ANNOTATION_CHEST_CONTAINER_LARGE_CALLBACK = "Linvtweaks/api/container/ChestContainer$IsLargeCallback;";
    private static final String ANNOTATION_INVENTORY_CONTAINER = "Linvtweaks/api/container/InventoryContainer;";
    private static final String ANNOTATION_IGNORE_CONTAINER = "Linvtweaks/api/container/IgnoreContainer;";
    private static final String ANNOTATION_CONTAINER_SECTION_CALLBACK = "Linvtweaks/api/container/ContainerSectionCallback;";
    private static final Logger logger = LogManager.getLogger("ASM InvTweaks");
    private static final Map<String, ContainerInfo> containerToTransform = new HashMap<>();
    private static final ClassConstantPoolParser classParser = new ClassConstantPoolParser(
            ANNOTATION_CHEST_CONTAINER,
            ANNOTATION_INVENTORY_CONTAINER,
            ANNOTATION_IGNORE_CONTAINER);

    public ContainerTransformer() {
        // TODO: ContainerCreative handling
        // Standard non-chest type
        containerToTransform.put(
                "net.minecraft.inventory.ContainerPlayer",
                new ContainerInfo(true, true, false, getVanillaSlotMapInfo("containerPlayerSlots")));
        containerToTransform.put("net.minecraft.inventory.ContainerMerchant", new ContainerInfo(true, true, false));
        containerToTransform.put(
                "net.minecraft.inventory.ContainerRepair",
                new ContainerInfo(true, true, false, getVanillaSlotMapInfo("containerPlayerSlots")));
        containerToTransform.put("net.minecraft.inventory.ContainerHopper", new ContainerInfo(true, true, false));
        containerToTransform.put("net.minecraft.inventory.ContainerBeacon", new ContainerInfo(true, true, false));
        containerToTransform.put(
                "net.minecraft.inventory.ContainerBrewingStand",
                new ContainerInfo(true, true, false, getVanillaSlotMapInfo("containerBrewingSlots")));
        containerToTransform.put(
                "net.minecraft.inventory.ContainerWorkbench",
                new ContainerInfo(true, true, false, getVanillaSlotMapInfo("containerWorkbenchSlots")));
        containerToTransform.put(
                "net.minecraft.inventory.ContainerEnchantment",
                new ContainerInfo(true, true, false, getVanillaSlotMapInfo("containerEnchantmentSlots")));
        containerToTransform.put(
                "net.minecraft.inventory.ContainerFurnace",
                new ContainerInfo(true, true, false, getVanillaSlotMapInfo("containerFurnaceSlots")));

        // Chest-type
        containerToTransform.put(
                "net.minecraft.inventory.ContainerDispenser",
                new ContainerInfo(true, false, true, (short) 3, getVanillaSlotMapInfo("containerChestDispenserSlots")));
        containerToTransform.put(
                "net.minecraft.inventory.ContainerChest",
                new ContainerInfo(true, false, true, getVanillaSlotMapInfo("containerChestDispenserSlots")));

        // Mod compatibility
        // Equivalent Exchange 3
        containerToTransform.put(
                "com.pahimar.ee3.inventory.ContainerAlchemicalBag",
                new ContainerInfo(true, false, true, true, (short) 13));
        containerToTransform.put(
                "com.pahimar.ee3.inventory.ContainerAlchemicalChest",
                new ContainerInfo(true, false, true, true, (short) 13));
        containerToTransform.put(
                "com.pahimar.ee3.inventory.ContainerPortableCrafting",
                new ContainerInfo(true, true, false, getCompatiblitySlotMapInfo("ee3PortableCraftingSlots")));

        // Ender Storage
        // TODO Row size method. A bit less important because it's a config setting and 2 of 3 options give rowsize 9.
        containerToTransform.put(
                "codechicken.enderstorage.storage.item.ContainerEnderItemStorage",
                new ContainerInfo(true, false, true));

        // Galacticraft
        containerToTransform.put(
                "micdoodle8.mods.galacticraft.core.inventory.GCCoreContainerPlayer",
                new ContainerInfo(true, true, false, getCompatiblitySlotMapInfo("galacticraftPlayerSlots")));

        try {
            containerToTransform.putAll(CompatibilityConfigLoader.load("config/InvTweaksCompatibility.xml"));
        } catch (FileNotFoundException ignored) {} catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        if ("net.minecraft.inventory.Container".equals(transformedName)) {
            logger.debug("Transforming " + transformedName);
            return transformContainer(basicClass);
        }

        if ("net.minecraft.client.gui.GuiTextField".equals(transformedName)) {
            logger.debug("Transforming " + transformedName);
            return transformGuiTextField(basicClass);
        }

        // Transform classes with explicitly specified information
        if (containerToTransform.containsKey(transformedName)) {
            logger.debug("Transforming " + transformedName);
            ClassReader classReader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            classReader.accept(classNode, 0);
            transformContainer(classNode, containerToTransform.get(transformedName));
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        }

        if (classParser.parse(basicClass)) {

            ClassReader cr = new ClassReader(basicClass);
            ClassNode cn = new ClassNode(Opcodes.ASM5);
            ClassWriter cw = new ClassWriter(0);

            cr.accept(cn, 0);

            if (cn.visibleAnnotations != null) {
                for (AnnotationNode annotation : cn.visibleAnnotations) {
                    if (annotation != null) {
                        ContainerInfo apiInfo = null;

                        if (ANNOTATION_CHEST_CONTAINER.equals(annotation.desc)) {
                            short rowSize = 9;
                            boolean isLargeChest = false;
                            boolean showButtons = true;

                            if (annotation.values != null) {
                                for (int i = 0; i < annotation.values.size(); i += 2) {
                                    String valueName = (String) annotation.values.get(i);
                                    Object value = annotation.values.get(i + 1);

                                    if ("rowSize".equals(valueName)) {
                                        rowSize = (short) ((Integer) value).intValue();
                                    } else if ("isLargeChest".equals(valueName)) {
                                        isLargeChest = (Boolean) value;
                                    } else if ("showButtons".equals(valueName)) {
                                        showButtons = (Boolean) value;
                                    }
                                }
                            }

                            apiInfo = new ContainerInfo(showButtons, false, true, isLargeChest, rowSize);

                            MethodNode row_method = findAnnotatedMethod(cn, ANNOTATION_CHEST_CONTAINER_ROW_CALLBACK);

                            if (row_method != null) {
                                apiInfo.rowSizeMethod = new MethodInfo(
                                        Type.getMethodType(row_method.desc),
                                        Type.getObjectType(cn.name),
                                        row_method.name);
                            }

                            MethodNode large_method = findAnnotatedMethod(
                                    cn,
                                    ANNOTATION_CHEST_CONTAINER_LARGE_CALLBACK);

                            if (large_method != null) {
                                apiInfo.largeChestMethod = new MethodInfo(
                                        Type.getMethodType(large_method.desc),
                                        Type.getObjectType(cn.name),
                                        large_method.name);
                            }
                        } else if (ANNOTATION_INVENTORY_CONTAINER.equals(annotation.desc)) {
                            boolean showOptions = true;

                            if (annotation.values != null) {
                                for (int i = 0; i < annotation.values.size(); i += 2) {
                                    String valueName = (String) annotation.values.get(i);
                                    Object value = annotation.values.get(i + 1);

                                    if ("showOptions".equals(valueName)) {
                                        showOptions = (Boolean) value;
                                    }
                                }
                            }

                            apiInfo = new ContainerInfo(showOptions, true, false);
                        } else if (ANNOTATION_IGNORE_CONTAINER.equals(annotation.desc)) {
                            // Annotation to restore default properties.
                            logger.debug("Transforming " + transformedName);
                            transformBaseContainer(cn);
                            cn.accept(cw);
                            return cw.toByteArray();
                        }

                        if (apiInfo != null) {
                            // Search methods to see if any have the ContainerSectionCallback attribute.
                            MethodNode method = findAnnotatedMethod(cn, ANNOTATION_CONTAINER_SECTION_CALLBACK);

                            if (method != null) {
                                apiInfo.slotMapMethod = new MethodInfo(
                                        Type.getMethodType(method.desc),
                                        Type.getObjectType(cn.name),
                                        method.name);
                            }

                            logger.debug("Transforming " + transformedName);
                            transformContainer(cn, apiInfo);
                            cn.accept(cw);
                            return cw.toByteArray();
                        }
                    }
                }
            }
        }

        return basicClass;
    }

    /**
     * Adds inventory tweaks methods to {@link net.minecraft.inventory.Container}
     */
    private static byte[] transformContainer(byte[] basicClass) {
        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        classReader.accept(classNode, 0);
        transformBaseContainer(classNode);
        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private MethodNode findAnnotatedMethod(ClassNode cn, String annotationDesc) {
        for (MethodNode method : cn.methods) {
            if (method.visibleAnnotations != null) {
                for (AnnotationNode methodAnnotation : method.visibleAnnotations) {
                    if (annotationDesc.equals(methodAnnotation.desc)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Alter class to contain information contained by ContainerInfo
     *
     * @param classNode Class to alter
     * @param info      Information used to alter class
     */
    public static void transformContainer(ClassNode classNode, ContainerInfo info) {
        classNode.interfaces.add(IINV_TWEAKS_CONTAINER_INTERFACE);
        ASMHelper.generateBooleanMethodConst(classNode, SHOW_BUTTONS_METHOD, info.showButtons);
        ASMHelper.generateBooleanMethodConst(classNode, VALID_INVENTORY_METHOD, info.validInventory);
        ASMHelper.generateBooleanMethodConst(classNode, VALID_CHEST_METHOD, info.validChest);

        if (info.largeChestMethod != null) {
            if (info.largeChestMethod.isStatic) {
                ASMHelper.generateForwardingToStaticMethod(
                        classNode,
                        LARGE_CHEST_METHOD,
                        info.largeChestMethod.methodName,
                        info.largeChestMethod.methodType.getReturnType(),
                        info.largeChestMethod.methodClass,
                        info.largeChestMethod.methodType.getArgumentTypes()[0]);
            } else {
                ASMHelper.generateSelfForwardingMethod(
                        classNode,
                        LARGE_CHEST_METHOD,
                        info.largeChestMethod.methodName,
                        info.largeChestMethod.methodType.getReturnType());
            }
        } else {
            ASMHelper.generateBooleanMethodConst(classNode, LARGE_CHEST_METHOD, info.largeChest);
        }

        if (info.rowSizeMethod != null) {
            if (info.rowSizeMethod.isStatic) {
                ASMHelper.generateForwardingToStaticMethod(
                        classNode,
                        ROW_SIZE_METHOD,
                        info.rowSizeMethod.methodName,
                        info.rowSizeMethod.methodType.getReturnType(),
                        info.rowSizeMethod.methodClass,
                        info.rowSizeMethod.methodType.getArgumentTypes()[0]);
            } else {
                ASMHelper.generateSelfForwardingMethod(
                        classNode,
                        ROW_SIZE_METHOD,
                        info.rowSizeMethod.methodName,
                        info.rowSizeMethod.methodType.getReturnType());
            }
        } else {
            ASMHelper.generateIntegerMethodConst(classNode, ROW_SIZE_METHOD, info.rowSize);
        }

        if (info.slotMapMethod.isStatic) {
            ASMHelper.generateForwardingToStaticMethod(
                    classNode,
                    SLOT_MAP_METHOD,
                    info.slotMapMethod.methodName,
                    info.slotMapMethod.methodType.getReturnType(),
                    info.slotMapMethod.methodClass,
                    info.slotMapMethod.methodType.getArgumentTypes()[0]);
        } else {
            ASMHelper.generateSelfForwardingMethod(
                    classNode,
                    SLOT_MAP_METHOD,
                    info.slotMapMethod.methodName,
                    info.slotMapMethod.methodType.getReturnType());
        }
    }

    /**
     * Alter class to contain default implementations of added methods.
     *
     * @param classNode Class to alter
     */
    public static void transformBaseContainer(ClassNode classNode) {
        classNode.interfaces.add(IINV_TWEAKS_CONTAINER_INTERFACE);
        ASMHelper.generateBooleanMethodConst(classNode, SHOW_BUTTONS_METHOD, false);
        ASMHelper.generateBooleanMethodConst(classNode, VALID_INVENTORY_METHOD, false);
        ASMHelper.generateBooleanMethodConst(classNode, VALID_CHEST_METHOD, false);
        ASMHelper.generateBooleanMethodConst(classNode, LARGE_CHEST_METHOD, false);
        ASMHelper.generateIntegerMethodConst(classNode, ROW_SIZE_METHOD, (short) 9);
        ASMHelper.generateForwardingToStaticMethod(
                classNode,
                SLOT_MAP_METHOD,
                "unknownContainerSlots",
                Type.getObjectType("java/util/Map"),
                Type.getObjectType(SLOT_MAPS_VANILLA_CLASS),
                Type.getObjectType(CONTAINER_CLASS_INTERNAL));
    }

    public static void transformCreativeContainer(ClassNode classNode) {
        /*
         * FIXME: Reqired methods cannot be compiled until SpecialSource update
         * ASMHelper.generateForwardingToStaticMethod(clazz, STANDARD_INVENTORY_METHOD, "containerCreativeIsInventory",
         * Type.BOOLEAN_TYPE, Type.getObjectType(SLOT_MAPS_VANILLA_CLASS));
         * ASMHelper.generateForwardingToStaticMethod(clazz, VALID_INVENTORY_METHOD, "containerCreativeIsInventory",
         * Type.BOOLEAN_TYPE, Type.getObjectType(SLOT_MAPS_VANILLA_CLASS)); ASMHelper.generateBooleanMethodConst(clazz,
         * VALID_CHEST_METHOD, false); ASMHelper.generateBooleanMethodConst(clazz, LARGE_CHEST_METHOD, false);
         * ASMHelper.generateIntegerMethodConst(clazz, ROW_SIZE_METHOD, (short) 9);
         * ASMHelper.generateForwardingToStaticMethod(clazz, SLOT_MAP_METHOD, "containerCreativeSlots",
         * Type.getObjectType("java/util/Map"), Type.getObjectType(SLOT_MAPS_VANILLA_CLASS));
         */
    }

    /**
     * Injects the call "InvTweaksMod.setTextboxModeStatic(var1);" at the head of the method
     * {@link net.minecraft.client.gui.GuiTextField#setFocused(boolean)}
     */
    private static byte[] transformGuiTextField(byte[] basicClass) {
        final ClassReader classReader = new ClassReader(basicClass);
        final ClassNode classNode = new ClassNode(Opcodes.ASM5);
        classReader.accept(classNode, 0);
        for (final MethodNode method : classNode.methods) {
            if ((method.name.equals("setFocused") || method.name.equals("b")) && method.desc.equals("(Z)V")) {
                final InsnList list = new InsnList();
                list.add(new VarInsnNode(Opcodes.ILOAD, 1));
                list.add(
                        new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "invtweaks/forge/InvTweaksMod",
                                "setTextboxModeStatic",
                                "(Z)V",
                                false));
                method.instructions.insert(list);
            }
        }
        final ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static MethodInfo getCompatiblitySlotMapInfo(String name) {
        return getSlotMapInfo(Type.getObjectType(SLOT_MAPS_MODCOMPAT_CLASS), name, true);
    }

    public static MethodInfo getVanillaSlotMapInfo(String name) {
        return getSlotMapInfo(Type.getObjectType(SLOT_MAPS_VANILLA_CLASS), name, true);
    }

    public static MethodInfo getSlotMapInfo(Type mClass, String name, boolean isStatic) {
        return new MethodInfo(
                Type.getMethodType(Type.getObjectType("java/util/Map"), Type.getObjectType(CONTAINER_CLASS_INTERNAL)),
                mClass,
                name,
                isStatic);
    }
}
