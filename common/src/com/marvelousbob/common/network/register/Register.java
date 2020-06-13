package com.marvelousbob.common.network.register;

import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.EndPoint;
import com.marvelousbob.common.events.PlayerConnection;
import com.marvelousbob.common.network.register.dto.*;

import java.util.ArrayList;


public final class Register {
    private Register() {
    }

    public static void registerClasses(EndPoint registrar) {
        registrar.getKryo().register(Msg.class);
        registrar.getKryo().register(Ping.class);
        registrar.getKryo().register(GameState.class);
        registrar.getKryo().register(MoveAction.class);
        registrar.getKryo().register(PlayerDto.class);
        registrar.getKryo().register(Array.class);
        registrar.getKryo().register(UUID.class);
        registrar.getKryo().register(GameIntialization.class);
        registrar.getKryo().register(PlayerConnection.class);
        registrar.getKryo().register(ArrayList.class);
    }
}
