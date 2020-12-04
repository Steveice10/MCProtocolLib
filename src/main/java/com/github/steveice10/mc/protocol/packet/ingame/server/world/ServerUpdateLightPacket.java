package com.github.steveice10.mc.protocol.packet.ingame.server.world;

import com.github.steveice10.mc.protocol.data.game.chunk.NibbleArray3d;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.io.IOException;
import java.util.Arrays;

@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
//TODO: Actually, like, make this work, instead of hacked together.
public class ServerUpdateLightPacket implements Packet {
    private static final int EMPTY_SIZE = 4096;
    private static final NibbleArray3d EMPTY = new NibbleArray3d(EMPTY_SIZE);

    private int x;
    private int z;
    private boolean trustEdges;
    private @NonNull NibbleArray3d[] skyLight;
    private @NonNull NibbleArray3d[] blockLight;

    public ServerUpdateLightPacket(int x, int z, boolean trustEdges, @NonNull NibbleArray3d[] skyLight, @NonNull NibbleArray3d[] blockLight) {
        this.x = x;
        this.z = z;
        this.trustEdges = trustEdges;
        this.skyLight = Arrays.copyOf(skyLight, skyLight.length);
        this.blockLight = Arrays.copyOf(blockLight, blockLight.length);
    }

    @Override
    public void read(NetInput in) throws IOException {
        this.x = in.readVarInt();
        this.z = in.readVarInt();
        this.trustEdges = in.readBoolean();

        long[] skyLightMask = in.readLongs(in.readVarInt());
        long[] blockLightMask = in.readLongs(in.readVarInt());
        long[] emptySkyLightMask = in.readLongs(in.readVarInt());
        long[] emptyBlockLightMask = in.readLongs(in.readVarInt());

        this.skyLight = new NibbleArray3d[in.readVarInt()];
        for (int i = 0; i < this.skyLight.length; i++) {
            if ((skyLightMask[0] & 1 << i) != 0) {
                this.skyLight[i] = new NibbleArray3d(in, in.readVarInt());
            } else if ((emptySkyLightMask[0] & 1 << i) != 0) {
                this.skyLight[i] = new NibbleArray3d(EMPTY_SIZE);
            } else {
                this.skyLight[i] = null;
            }
        }

        this.blockLight = new NibbleArray3d[in.readVarInt()];
        for (int i = 0; i < this.blockLight.length; i++) {
            if ((blockLightMask[0] & 1 << i) != 0) {
                this.blockLight[i] = new NibbleArray3d(in, in.readVarInt());
            } else if ((emptyBlockLightMask[0] & 1 << i) != 0) {
                this.blockLight[i] = new NibbleArray3d(EMPTY_SIZE);
            } else {
                this.blockLight[i] = null;
            }
        }
    }

    @Override
    public void write(NetOutput out) throws IOException {
        out.writeVarInt(this.x);
        out.writeVarInt(this.z);
        out.writeBoolean(this.trustEdges);

        int skyLightMask = 0;
        int blockLightMask = 0;
        int emptySkyLightMask = 0;
        int emptyBlockLightMask = 0;

        for (int i = 0; i < this.skyLight.length; i++) {
            NibbleArray3d skyLight = this.skyLight[i];
            if(skyLight != null) {
                if(EMPTY.equals(skyLight)) {
                    emptySkyLightMask |= 1 << i;
                } else {
                    skyLightMask |= 1 << i;
                }
            }
        }

        for (int i = 0; i < this.blockLight.length; i++) {
            NibbleArray3d blockLight = this.blockLight[i];
            if(blockLight != null) {
                if(EMPTY.equals(blockLight)) {
                    emptyBlockLightMask |= 1 << i;
                } else {
                    blockLightMask |= 1 << i;
                }
            }
        }

        out.writeVarInt(1);
        out.writeLong(skyLightMask);
        out.writeVarInt(1);
        out.writeLong(blockLightMask);
        out.writeVarInt(1);
        out.writeLong(emptySkyLightMask);
        out.writeVarInt(1);
        out.writeLong(emptyBlockLightMask);

        out.writeVarInt(this.skyLight.length);
        for(int i = 0; i < this.skyLight.length; i++) {
            if((skyLightMask & 1 << i) != 0) {
                out.writeVarInt(this.skyLight[i].getData().length);
                this.skyLight[i].write(out);
            }
        }

        out.writeVarInt(this.blockLight.length);
        for(int i = 0; i < this.blockLight.length; i++) {
            if((blockLightMask & 1 << i) != 0) {
                out.writeVarInt(this.blockLight[i].getData().length);
                this.blockLight[i].write(out);
            }
        }
    }

    @Override
    public boolean isPriority() {
        return false;
    }
}
