package com.qzero.mcga.data

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class ModInfo(
    /**
     * Mod的唯一ID，例如create
     */
    @Id
    var modeId: String,
    /**
     * Mod的名称，用于模糊搜索，例如：机械动力
     */
    var modName: String,
    /**
     * 加载器类型，只有两个选项：fabric, forge
     */
    var loaderType: String,
    /**
     * Mod版本号Tag，用于指定版本号时使用
     */
    var modVersionTag: String,
    /**
     * Mod版本号序列，对于同一个ID的Mod，版本号越新，序列号越大
     */
    var modVersionSeq: Int,
    /**
     * 适配的游戏版本号，例如1.20.1
     */
    var gameVersion: String,
    /**
     * Mod对应的对象存储信息
     */
    @Embedded
    var cosObjectInfo: CosObject,
) {
    constructor(): this("", "", "", "", 0, "", CosObject())
}
