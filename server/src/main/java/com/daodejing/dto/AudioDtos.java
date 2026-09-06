package com.daodejing.dto;

public class AudioDtos {

    /** 音色概览：标识、名称、已入库章节数 */
    public record AudioVoiceVO(String voiceKey, String voiceName, int count) {}
}
