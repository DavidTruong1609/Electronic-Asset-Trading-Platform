package ElectronicAssetTradingPlatform.AssetTrading;

import ElectronicAssetTradingPlatform.Server.NetworkDataSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Class for creating a sell offer and resolving the sell offer after it has been created
 */
public class SellOffer extends Offer {

    /**
     * Constructor for trade offer
     *
     * @param asset                  Name of the asset to be bought or sold
     * @param quantity               Quantity of asset
     * @param pricePerUnit           Price of the asset
     * @param username               The ID of the user who made the offer
     * @param organisationalUnitName The ID of the organisation whose assets and credits will be affected
     */
    public SellOffer(String asset, int quantity, double pricePerUnit, String username, String organisationalUnitName) {
        super(asset, quantity, pricePerUnit, username, organisationalUnitName);
    }

    /**
     * Overloaded Constructor for trade offer - used when retrieving offer from DB including the sell order ID
     * @param orderID                The ID of the offer
     * @param asset                  Name of the asset to be bought or sold
     * @param quantity               Quantity of asset
     * @param pricePerUnit           Price of the asset
     * @param username               The ID of the user who made the offer
     * @param organisationalUnitName The ID of the organisation whose assets and credits will be affected
     */
    public SellOffer(int orderID, String asset, int quantity, double pricePerUnit, String username, String organisationalUnitName) {
        super(orderID, asset, quantity, pricePerUnit, username, organisationalUnitName);
    }

    /**
     * Returns an array list of all buy offers which are offering the same asset (asset name's are the same)
     *
     * @return Array List of matching Buy Offers
     */
    private ArrayList<BuyOffer> matchingBuyOffers() {
        ArrayList<BuyOffer> matchingBuyOffers = new ArrayList<>();
        // retrieve buy offers from the database
        Map<Integer, BuyOffer> buyOfferMap = BuyOfferData.getInstance().getMarketBuyOffers();
        // return buy offers whose asset name are the same as the sell offer's asset name
        for (Map.Entry<Integer, BuyOffer> buyOffer : buyOfferMap.entrySet()) {
            if (sameAssetName(buyOffer.getValue(), this)) {
                BuyOffer matchingOffer = buyOffer.getValue();
                matchingBuyOffers.add(matchingOffer);
            }
        }
        return matchingBuyOffers;
    }

    /**
     * Takes the matching buy offers and finds a buy offer which is equally or higher priced than the sell offer.
     * As long as the buy offer is equally or higher priced, it will be matched and returned (a higher priced buy offer
     * does not have priority because the trades occur at the sell price).
     * Thus, the returned buy offer will the oldest buy offer which is equally or higher priced
     *
     * @return int of the buy offer, int >= 0
     */
    private int getMatchedPriceOffer() {
        ArrayList<BuyOffer> matchingBuyOffers = matchingBuyOffers();
        double sellOfferPrice = getPricePerUnit();
        Iterator<BuyOffer> buyOffersIter = matchingBuyOffers.iterator();
        BuyOffer buyOffer;
        double buyOfferPrice;
        if (buyOffersIter.hasNext()) {
            // iterate through the matching buy offers, returning the first buy offer ID whose price is equal or greater
            // than the sell offer's price
            while (buyOffersIter.hasNext()) {
                buyOffer = buyOffersIter.next();
                buyOfferPrice = buyOffer.getPricePerUnit();
                if (buyOfferPrice >= sellOfferPrice) {
                    return buyOffer.getOfferID();
                }
            }
        }
        else { // if no buy offer with matching asset name return 0
            return NO_MATCHING_OFFERS;
        }
        return NO_MATCHING_OFFERS; // if no matching buy offer with equal or greater price than sell offer return 0
    }


    /**
     * Takes a matching buy offer ID and compares it to the sell offer
     * Then it reduces the 'quantities' of both offers
     * Also inserts into the asset history the buy and sell offer as well as the quantity traded
     *
     * @param matchingID matching buy offer ID
     */
    private void reduceMatchingOfferQuantities(int matchingID) {
        NetworkDataSource data = new NetworkDataSource();
        data.run();
        if (isMatching(matchingID)) {
            BuyOffer matchingBuyOffer = BuyOfferData.getInstance().getOffer(matchingID);
            int sellOfferQuantity = this.getQuantity();
            int buyOfferQuantity = matchingBuyOffer.getQuantity();
            // if the quantity of buy and sell offers are equal remove them both from the DB
            if (sellOfferQuantity == buyOfferQuantity) {
                BuyOfferData.getInstance().removeOffer(this.getOfferID());
                SellOfferData.getInstance().removeOffer(matchingID);
                this.setQuantity(0);
                data.addAssetHistory(matchingBuyOffer, this, sellOfferQuantity);
            }
            // if the quantity specified by the buy offer is greater than the sell offer, remove the sell offer from DB
            // and reduce the quantity of the buy offer by the quantity of the sell offer
            else if (sellOfferQuantity > buyOfferQuantity) {
                int updatedSellQuantity = sellOfferQuantity - buyOfferQuantity;
                // update the database with new quantity
                SellOfferData.getInstance().updateOfferQuantity(updatedSellQuantity, this.getOfferID());
                BuyOfferData.getInstance().removeOffer(matchingID);
                this.setQuantity(updatedSellQuantity);
                data.addAssetHistory(matchingBuyOffer, this, buyOfferQuantity);
            }
            // if the quantity specified by the buy offers is less than the sell offers, remove the buy offer from DB
            // and reduce the quantity of the sell offer by the quantity of the buy offer
            else {
                int updatedBuyQuantity = buyOfferQuantity - sellOfferQuantity;
                BuyOfferData.getInstance().updateOfferQuantity(updatedBuyQuantity, matchingID);
                SellOfferData.getInstance().removeOffer(this.getOfferID());
                this.setQuantity(0);
                data.addAssetHistory(matchingBuyOffer, this, sellOfferQuantity);
            }
        }
    }

    /**
     * Remove credits from the buy org and add credits the sell org
     *
     * @param credit amount of credits to be removed or added
     * @param buyOrgName the matching buy offer's org unit name
     */
    private void tradeCredits(double credit, String buyOrgName) {
        NetworkDataSource dataSource = new NetworkDataSource();
        dataSource.run();
        // increase credits of the sell org
        dataSource.editCredits(credit, this.getUnitName());
        // decrease credits of the buy org
        dataSource.editCredits(-(credit), buyOrgName);
    }

    /**
     * Remove assets from the sell org and add them to the buy org
     * @param buyOffer the matching buy offer
     * @param quantity quantity to be added to the buy org and removed from the sell org
     */
    private void tradeAssets(int quantity, BuyOffer buyOffer)  {
        NetworkDataSource dataSource = new NetworkDataSource();
        dataSource.run();
        if (!orgOwnsAsset(buyOffer)) {
            OrganisationalUnit unit = new OrganisationalUnit(buyOffer.getUnitName(), 0);
            dataSource.setOrgUnitAssets(unit, buyOffer.getAssetName(), 0);
        }
        dataSource.editAssets(-(quantity), this.getUnitName(), this.getAssetName());
        dataSource.editAssets(quantity, buyOffer.getUnitName(), buyOffer.getAssetName());
    }

    /**
     * Exchanges the organisational unit assets and credits with a matching buy offer
     * @param matchingID - ID of the matching buy offer
     */
    private void tradeAssetsAndCredits(int matchingID) {
        if (isMatching(matchingID)) {
            BuyOffer matchingBuyOffer = BuyOfferData.getInstance().getOffer(matchingID);
            int buyOfferQuantity = matchingBuyOffer.getQuantity();
            int  sellOfferQuantity = this.getQuantity();
            double creditsExchanged;
            int assetsExchanged;
            double sellersPrice = this.getPricePerUnit();
            // determine assets and credits to be traded
            if (sellOfferQuantity > buyOfferQuantity) {
                creditsExchanged = sellersPrice * buyOfferQuantity;
                assetsExchanged = buyOfferQuantity;
            }
            else {
                assetsExchanged = sellOfferQuantity;
                creditsExchanged = sellersPrice * sellOfferQuantity;
            }
            tradeAssets(assetsExchanged, matchingBuyOffer);
            tradeCredits(creditsExchanged, matchingBuyOffer.getUnitName());
        }
    }

    /**
     * Compares the created sell offer with all buy offers, finding offers with the same asset name and appropriate price
     * Then proceeds to trade assets and credits, whilst updating the offer quantities or removing them (if fully resolved)
     * Repeats this process until the sell offer has been fully resolved OR there are no more matching buy offers
     *
     * @return NOT_RESOLVED if no trades occured, PARTIALLY resolved if part of the offer was resolved, and
     * FULLY_RESOLVED if the sell offer is fully resolved
     */
    public int resolveOffer() {
        // loop until there is no matching offer OR this.quantity == 0
        boolean sellOfferNotResolved = SellOfferData.getInstance().offerExists(this.getOfferID());
        int matchingID = getMatchedPriceOffer();
        if (!isMatching(matchingID)) {
            return NOT_RESOLVED;
        }
        while (isMatching(matchingID) && sellOfferNotResolved) {
            matchingID = getMatchedPriceOffer();
            // reduce the quantities of matching buy and sell offers + deleting offers if they've been fully resolved
            tradeAssetsAndCredits(matchingID);
            reduceMatchingOfferQuantities(matchingID);
            // probably create a match offer history here whenever assets are traded
            sellOfferNotResolved = SellOfferData.getInstance().offerExists(this.getOfferID());
            if (!sellOfferNotResolved) {
                return FULLY_RESOLVED;
            }
        }
        return PARTIALLY_RESOLVED;
    }
}
