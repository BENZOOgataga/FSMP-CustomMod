package com.flazesmp.companies;

import com.flazesmp.companies.client.gui.CompanyCreationScreen;
import com.flazesmp.companies.client.gui.MarketScreen;
import com.flazesmp.companies.common.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.commands.Commands;

@Mod(FlazeSMP.MOD_ID)
public class FlazeSMP {
    public static final String MOD_ID = "flazesmp";
    public static final Logger LOGGER = LogManager.getLogger();

    public FlazeSMP() {
        // Register the setup method for modloading
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        // Only register client setup on the client side
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Common setup code
        LOGGER.info("FlazeSMP: Common setup");

        // Initialize network system - CRITICAL for client-server communication
        event.enqueueWork(() -> {
            NetworkHandler.init();
            LOGGER.info("Network system initialized");
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Client-side setup code
        LOGGER.info("FlazeSMP: Client setup");

        // Register client-side event handlers
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.class);
    }

    // Server-side command registration
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Only register server-side commands here
        LOGGER.info("Registering server-side commands");
    }

    // Client-side event handler and command registration
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEventHandler {

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            LOGGER.info("Registering client-side commands");

            // Register the /company create command on client side
            event.getDispatcher().register(
                    Commands.literal("companycreate")
                            .executes(context -> {
                                openCompanyCreationScreen();
                                return 1;
                            })
            );

            event.getDispatcher().register(
                    Commands.literal("MarketOpen")
                            .executes(context -> {
                                openMarketScreen();
                                return 1;
                            })
            );


        }

        // Method to open the company creation screen
        private static void openCompanyCreationScreen() {
            Minecraft.getInstance().setScreen(new CompanyCreationScreen());
        }

        private static void openMarketScreen() {
            Minecraft.getInstance().setScreen(new MarketScreen());
        }
    }
}
