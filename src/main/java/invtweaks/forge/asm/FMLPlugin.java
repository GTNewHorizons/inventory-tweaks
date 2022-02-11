package invtweaks.forge.asm;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;

import java.util.Map;

@TransformerExclusions({"invtweaks.forge.asm", "invtweaks.forge.asm.compatibility"})
@MCVersion("1.7.10")
public class FMLPlugin implements IFMLLoadingPlugin {
    public static boolean runtimeDeobfEnabled = false;

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"invtweaks.forge.asm.ContainerTransformer"};
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
        runtimeDeobfEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
    }
}
