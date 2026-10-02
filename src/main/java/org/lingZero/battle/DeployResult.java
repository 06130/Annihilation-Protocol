package org.lingZero.battle;

/**
 * 部署请求的判定结果。失败原因全部由服务端产生，客户端只负责展示。
 */
public enum DeployResult {
    SUCCESS,
    NO_SESSION,
    NOT_CONFIGURED,
    INVALID_STATE,
    WRONG_DIMENSION,
    TOO_FAR,
    OUT_OF_AREA,
    OCCUPIED,
    ON_PATH,
    LIMIT_REACHED,
    COOLDOWN,
    INVALID_TYPE,
    /** 服务端内部失败（实体创建/加入世界失败），与玩家操作无关。 */
    SERVER_ERROR
}
