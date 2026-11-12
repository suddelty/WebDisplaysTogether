/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.net.server_bound.C2SMessageMiniservConnect;

public class S2CMessageServerInfo extends Packet {
	
	private int miniservPort;
	// The server's URL lists, so client-side browsers (minePads) follow them too
	private String[] blacklist;
	private String[] whitelist;
	
	public S2CMessageServerInfo(int msPort, String[] blacklist, String[] whitelist) {
		miniservPort = msPort;
		this.blacklist = blacklist != null ? blacklist : new String[0];
		this.whitelist = whitelist != null ? whitelist : new String[0];
	}
	
	public S2CMessageServerInfo(FriendlyByteBuf buf) {
		super(buf);
		miniservPort = buf.readShort();
		blacklist = readList(buf);
		whitelist = readList(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeShort(miniservPort);
		writeList(buf, blacklist);
		writeList(buf, whitelist);
	}
	
	private static void writeList(FriendlyByteBuf buf, String[] list) {
		buf.writeVarInt(list.length);
		for (String s : list)
			buf.writeUtf(s != null ? s : "");
	}
	
	private static String[] readList(FriendlyByteBuf buf) {
		int n = buf.readVarInt();
		if (n < 0 || n > 4096)
			throw new IllegalArgumentException("Bad URL list size: " + n);
		String[] list = new String[n];
		for (int i = 0; i < n; i++)
			list[i] = buf.readUtf();
		return list;
	}
	
	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (checkClient(ctx)) {
			WebDisplays.setRemoteUrlLists(blacklist, whitelist);
			try {
				WebDisplays.PROXY.setMiniservClientPort(miniservPort);
				C2SMessageMiniservConnect message = Client.getInstance().beginConnection();
				respond(ctx, message);
				ctx.setPacketHandled(true);
			} catch (Throwable err) {
				err.printStackTrace();
				throw new RuntimeException(err);
			}
		}
	}
}
