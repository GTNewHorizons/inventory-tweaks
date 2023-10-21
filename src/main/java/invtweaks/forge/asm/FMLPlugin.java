package invtweaks.forge.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@MCVersion("1.7.10")
@TransformerExclusions({ "invtweaks.forge.asm" })
public class FMLPlugin implements IFMLLoadingPlugin {

    private static boolean isObf = false;

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "invtweaks.forge.asm.ContainerTransformer" };
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        isObf = (boolean) data.get("runtimeDeobfuscationEnabled");
    }

    public static boolean isObf() {
        return isObf;
    }
}
