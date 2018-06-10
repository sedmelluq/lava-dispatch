package com.sedmelluq.lava.discord.dispatch;

import com.sedmelluq.lava.discord.dispatch.packet.AudioPacketProvider;

public interface AudioSendSystemFactory {
  AudioSendSystem create(AudioPacketProvider packetProvider);
}
