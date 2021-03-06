package ElectronicAssetTradingPlatform.Server;

/**
 * Commands used by the NetworkServer and NetworkDataSource to send/receive data
 */
public enum NetworkCommands {
    RETRIEVE_USER,
    STORE_USER,
    EDIT_USER,
    EDIT_PASSWORD,
    ADD_BUY_OFFER,
    ADD_SELL_OFFER,
    REMOVE_OFFER,
    GET_BUY_OFFERS,
    GET_SELL_OFFERS,
    GET_PLACED_OFFER,
    UPDATE_CREDITS,
    UPDATE_ASSETS,
    UPDATE_OFFER,
    QUANTITY,
    GET_UNIT_CREDIT,
    GET_UNIT_ASSETS,
    ADD_HISTORY,
    STORE_ORG_UNIT,
    STORE_ASSET,
    GET_ASSET_HISTORY,
    RETRIEVE_ORG_UNIT,
    SET_ORG_UNIT_CREDITS,
    SET_ORG_UNIT_ASSETS,
    GET_ALL_ASSETS,
    GET_ALL_MEMBERS,
    EDIT_ORG_UNIT_NAME,
    RETRIEVE_ASSET,
    EDIT_ASSET_NAME,
    GET_UNIT_TRADEHISTORY
}
