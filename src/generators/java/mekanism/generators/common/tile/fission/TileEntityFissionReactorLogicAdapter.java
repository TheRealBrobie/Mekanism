package mekanism.generators.common.tile.fission;

import javax.annotation.Nonnull;
import mekanism.api.NBTConstants;
import mekanism.api.math.MathUtils;
import mekanism.api.text.EnumColor;
import mekanism.api.text.IHasTranslationKey;
import mekanism.common.MekanismLang;
import mekanism.common.base.ILangEntry;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.util.NBTUtils;
import mekanism.generators.common.GeneratorsLang;
import mekanism.generators.common.base.IReactorLogic;
import mekanism.generators.common.base.IReactorLogicMode;
import mekanism.generators.common.registries.GeneratorsBlocks;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorLogicAdapter.FissionReactorLogic;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class TileEntityFissionReactorLogicAdapter extends TileEntityFissionReactorCasing implements IReactorLogic<FissionReactorLogic> {

    public FissionReactorLogic logicType = FissionReactorLogic.DISABLED;
    private RedstoneStatus prevStatus = RedstoneStatus.IDLE;

    public TileEntityFissionReactorLogicAdapter() {
        super(GeneratorsBlocks.FISSION_REACTOR_LOGIC_ADAPTER);
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        RedstoneStatus status = getStatus();
        if (status != prevStatus) {
            World world = getWorld();
            if (world != null) {
                world.notifyNeighborsOfStateChange(getPos(), getBlockType());
            }
        }
        prevStatus = status;
    }

    @Override
    public FissionReactorLogic getMode() {
        return logicType;
    }

    public RedstoneStatus getStatus() {
        if (isRemote()) {
            return prevStatus;
        }
        if (structure != null) {
            switch (logicType) {
                case ACTIVATION:
                    return isPowered() ? RedstoneStatus.POWERED : RedstoneStatus.IDLE;
                case EXCESS_WASTE:
                    return structure.wasteTank.getNeeded() == 0 ? RedstoneStatus.OUTPUTTING : RedstoneStatus.IDLE;
                case DEPLETED:
                    return structure.fuelTank.isEmpty() ? RedstoneStatus.OUTPUTTING : RedstoneStatus.IDLE;
                default: break;
            }
        }
        return RedstoneStatus.IDLE;
    }

    public void setLogicTypeFromPacket(FissionReactorLogic logicType) {
        this.logicType = logicType;
        markDirty(false);
    }

    @Override
    public void onPowerChange() {
        super.onPowerChange();
        if (!isRemote() && structure != null) {
            if (logicType == FissionReactorLogic.ACTIVATION) {
                structure.active = isPowered();
            }
        }
    }

    @Override
    public void read(CompoundNBT nbtTags) {
        super.read(nbtTags);
        NBTUtils.setEnumIfPresent(nbtTags, NBTConstants.LOGIC_TYPE, FissionReactorLogic::byIndexStatic, logicType -> this.logicType = logicType);
    }

    @Nonnull
    @Override
    public CompoundNBT write(CompoundNBT nbtTags) {
        super.write(nbtTags);
        nbtTags.putInt(NBTConstants.LOGIC_TYPE, logicType.ordinal());
        return nbtTags;
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(FissionReactorLogic::byIndexStatic, FissionReactorLogic.DISABLED, () -> logicType, value -> logicType = value));
    }

    public enum FissionReactorLogic implements IReactorLogicMode, IHasTranslationKey {
        DISABLED(GeneratorsLang.REACTOR_LOGIC_DISABLED, GeneratorsLang.DESCRIPTION_REACTOR_DISABLED, new ItemStack(Items.GUNPOWDER), EnumColor.DARK_GRAY),
        ACTIVATION(GeneratorsLang.REACTOR_LOGIC_ACTIVATION, GeneratorsLang.DESCRIPTION_REACTOR_ACTIVATION, new ItemStack(Items.FLINT_AND_STEEL), EnumColor.AQUA),
        EXCESS_WASTE(GeneratorsLang.REACTOR_LOGIC_EXCESS_WASTE, GeneratorsLang.DESCRIPTION_REACTOR_EXCESS_WASTE, new ItemStack(Items.REDSTONE), EnumColor.RED),
        DEPLETED(GeneratorsLang.REACTOR_LOGIC_DEPLETED, GeneratorsLang.DESCRIPTION_REACTOR_DEPLETED, new ItemStack(Items.REDSTONE), EnumColor.RED);

        private static final FissionReactorLogic[] MODES = values();

        private final ILangEntry name;
        private final ILangEntry description;
        private final ItemStack renderStack;
        private final EnumColor color;

        FissionReactorLogic(ILangEntry name, ILangEntry description, ItemStack stack, EnumColor color) {
            this.name = name;
            this.description = description;
            renderStack = stack;
            this.color = color;
        }

        @Override
        public ItemStack getRenderStack() {
            return renderStack;
        }

        @Override
        public String getTranslationKey() {
            return name.getTranslationKey();
        }

        @Override
        public ITextComponent getDescription() {
            return description.translate();
        }

        @Override
        public EnumColor getColor() {
            return color;
        }

        public static FissionReactorLogic byIndexStatic(int index) {
            return MathUtils.getByIndexMod(MODES, index);
        }
    }

    public enum RedstoneStatus implements IHasTranslationKey {
        IDLE(MekanismLang.IDLE),
        OUTPUTTING(GeneratorsLang.REACTOR_LOGIC_OUTPUTTING),
        POWERED(GeneratorsLang.REACTOR_LOGIC_POWERED);

        private ILangEntry name;

        private RedstoneStatus(ILangEntry name) {
            this.name = name;
        }

        @Override
        public String getTranslationKey() {
            return name.getTranslationKey();
        }
    }
}
