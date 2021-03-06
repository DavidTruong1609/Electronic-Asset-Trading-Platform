package ElectronicAssetTradingPlatform.AssetTrading;

import ElectronicAssetTradingPlatform.Server.NetworkDataSource;
import java.util.*;

/**
 * Class for creating a buy offer and resolving the buy offer after it has been created
 */
public class BuyOffer extends Offer  {
    /**
     * Constructor for trade offer
     * @param asset                  Name of the asset to be bought or sold
     * @param quantity               Quantity of asset
     * @param pricePerUnit           Price of the asset
     * @param username               The ID of the user who made the offer
     * @param organisationalUnitName The ID of the organisation whose assets and credits will be affected
     */
    public BuyOffer(String asset, int quantity, double pricePerUnit, String username, String organisationalUnitName) {
        super(asset, quantity, pricePerUnit, username, organisationalUnitName);
    }

    /**
     * Overloaded Constructor for trade offer - used when retrieving offer from DB which includes the orderID
     * @param orderID                The ID of the offer
     * @param asset                  Name of the asset to be bought or sold
     * @param quantity               Quantity of asset
     * @param pricePerUnit           Price of the asset
     * @param username               The ID of the user who made the offer
     * @param organisationalUnitName The ID of the organisation whose assets and credits will be affected
     */
    public BuyOffer(int orderID, String asset, int quantity, double pricePerUnit, String username, String organisationalUnitName) {
        super(orderID, asset, quantity, pricePerUnit, username, organisationalUnitName);
    }

    /**
     * Returns an array list of all sell offers which are offering the same asset (asset name's are the same)
     *
     * @return Array List of matching Sell Offers
     */
    private ArrayList<SellOffer> getMatchingSellOffers() {
        ArrayList<SellOffer> matchingSellOffers = new ArrayList<>();
        // retrieve all sell offers from the database
        Map<Integer, SellOffer> sellOfferMap = SellOfferData.getInstance().getMarketSellOffers();
        // compare the sell offer's asset name to the buy offer's asset name, adding those which match
        for (Map.Entry<Integer, SellOffer> sellOffer : sellOfferMap.entrySet()) {
            if (sameAssetName(this, sellOffer.getValue())) {
                SellOffer matchingOffer = sellOffer.getValue();
                matchingSellOffers.add(matchingOffer);
            }
        }
        return matchingSellOffers;
    }

    /**
     * Takes the matching sell offers and finds the lowest priced sell offer which is equally or lower priced than
     * the buy offer.
     * If two sell offers are equally priced, the offer placed first has priority (this will be the offer queried first)
     *
     * @return int of the sell offer OR 0 if no sell offer was found with a matching price and asset name
     */
    private int getMatchedPriceOffer() {
        ArrayList<SellOffer> matchingSellOffers = getMatchingSellOffers();
        double buyOfferPrice = getPricePerUnit();
        Iterator<SellOffer> sellOffersIter = matchingSellOffers.iterator();
        SellOffer lowestSellOffer;
        double lowestSellPrice;
        if (sellOffersIter.hasNext()) {
            // assign the lowest sell offer and price to the first sell offer
            lowestSellOffer = sellOffersIter.next();
            lowestSellPrice = lowestSellOffer.getPricePerUnit();
            // iterate through matching sell offers to see which has the lowest price
            while(sellOffersIter.hasNext()) {
                SellOffer newOffer = sellOffersIter.next();
                double newOfferPrice = newOffer.getPricePerUnit();
                if (newOfferPrice < lowestSellPrice) {
                    lowestSellPrice = newOffer.getPricePerUnit();
                    lowestSellOffer = newOffer;
                }
            }
            // return the sell offer's ID if the lowest matching sell offer is equal or less than they buy offer's price
            if (lowestSellPrice <= buyOfferPrice) {
                return lowestSellOffer.getOfferID();
            }
            else {
                return NO_MATCHING_OFFERS;
            }
        }
        else {
            return NO_MATCHING_OFFERS;
        }
    }

    /**
     * Takes a matching sell offer ID and compares it to the buy offer
     * Then it reduces the 'quantities' of both offers
     * Also inserts into the asset history the buy and sell offer as well as the quantity traded
     *
     * @param matchingID matching sell offer ID
     */
    private void reduceMatchingOfferQuantities(int matchingID) {
        if (isMatching(matchingID)) {
            NetworkDataSource data = new NetworkDataSource();
            data.run();
            SellOffer matchingSellOffer = SellOfferData.getInstance().getOffer(matchingID);
            int sellOfferQuantity = matchingSellOffer.getQuantity();
            int buyOfferQuantity = this.getQuantity();
            // if the quantity specified by matching buy and sell offers are equal remove them both from the DB
            if (buyOfferQuantity == sellOfferQuantity) {
                BuyOfferData.getInstance().removeOffer(this.getOfferID());
                SellOfferData.getInstance().removeOffer(matchingID);
                data.addAssetHistory(this, matchingSellOffer, sellOfferQuantity);
                this.setQuantity(0);
            }
            // if the quantity specified by the buy offer is greater than the sell offer, remove the sell offer from DB
            // and reduce the quantity of the buy offer by the quantity of the sell offer
            else if (buyOfferQuantity > sellOfferQuantity) {
                int updatedBuyQuantity = buyOfferQuantity - sellOfferQuantity;
                BuyOfferData.getInstance().updateOfferQuantity(updatedBuyQuantity, this.getOfferID());
                SellOfferData.getInstance().removeOffer(matchingID);
                data.addAssetHistory(this, matchingSellOffer, sellOfferQuantity);
                this.setQuantity(updatedBuyQuantity);

            }
            // if the quantity specified by the buy offers is less than the sell offers, remove the buy offer from DB
            // and reduce the quantity of the sell offer by the quantity of the buy offer
            else {
                int updatedSellQuantity = sellOfferQuantity - buyOfferQuantity;
                SellOfferData.getInstance().updateOfferQuantity(updatedSellQuantity, matchingID);
                BuyOfferData.getInstance().removeOffer(this.getOfferID());
                data.addAssetHistory(this, matchingSellOffer, buyOfferQuantity);
                this.setQuantity(0);
            }
        }
    }

    /**
     * Remove credits from the buy org and add credits the sell org
     *
     * @param credit amount of credits to increase/decrease
     * @param sellOrgName sell organisational unit to insert credits into
     */
    private void tradeCredits(double credit, String sellOrgName) {
        NetworkDataSource dataSource = new NetworkDataSource();
        dataSource.run();
        // increase credits of sell org
        dataSource.editCredits(credit, sellOrgName);
        // decrease credits of buy org
        dataSource.editCredits(-(credit), this.getUnitName());
    }

    /**
     * Remove assets from the sell org and add them to the buy org
     * Also will insert the asset into the org unit if they do not own any copies yet in the DB
     */
    private void tradeAssets(int quantity, SellOffer sellOffer)  {
        NetworkDataSource dataSource = new NetworkDataSource();
        dataSource.run();
        // if the buy org unit does not own the asset yet, add that asset to their unit with quantity 0 initially
        if (!orgOwnsAsset(this)) {
            OrganisationalUnit unit = new OrganisationalUnit(this.getUnitName(), 0);
            dataSource.setOrgUnitAssets(unit, this.getAssetName(), 0);
        }
        // add assets to the buy unit
        dataSource.editAssets(quantity, this.getUnitName(), this.getAssetName());
        // remove assets from the sell unit
        dataSource.editAssets(-(quantity), sellOffer.getUnitName(), sellOffer.getAssetName());
    }

    /**
     * Exchanges the organisational unit assets and credits with a matching sell offer
     * @param matchingID - ID of the matching sell offer
     */
    private void tradeAssetsAndCredits(int matchingID)  {
        if (isMatching(matchingID)) {
            SellOffer matchingSellOffer = SellOfferData.getInstance().getOffer(matchingID);
            int sellOfferQuantity = matchingSellOffer.getQuantity();
            int buyOfferQuantity = this.getQuantity();
            double creditsExchanged;
            int assetsExchanged;
            double sellersPrice = matchingSellOffer.getPricePerUnit();
            // determine assets and credits to be traded
            if (buyOfferQuantity > sellOfferQuantity) {
                creditsExchanged = sellersPrice * sellOfferQuantity;
                assetsExchanged = sellOfferQuantity;
            }
            else {
                assetsExchanged = buyOfferQuantity;
                creditsExchanged = sellersPrice * buyOfferQuantity;
            }
            tradeAssets(assetsExchanged, matchingSellOffer);
            tradeCredits(creditsExchanged, matchingSellOffer.getUnitName());
        }
    }

    /**
     * Compares the created buy offer with all sell offers, finding offers with the same asset name and appropriate price
     * Then proceeds to trade assets and credits, whilst updating the offer quantities or removing them (if fully resolved)
     * Repeats this process until the buy offer has been fully resolved OR there are no more matching sell offers
     *
     * @return NOT_RESOLVED if no trades occured, PARTIALLY resolved if part of the offer was resolved, and
     * FULLY_RESOLVED if the buy offer is fully resolved
     */
    public int resolveOffer() {
        // loop if there is a matching offer and if the offer has not been fully resolved
        boolean buyOfferNotResolved = BuyOfferData.getInstance().offerExists(this.getOfferID());
        int matchingID = getMatchedPriceOffer();
        if (!isMatching(matchingID)) {
            return NOT_RESOLVED;
        }
        while (isMatching(matchingID) && buyOfferNotResolved) {
            matchingID = getMatchedPriceOffer();
            // trade assets and credits of the matching offers
            tradeAssetsAndCredits(matchingID);
            // edit the quantity of the offers
            reduceMatchingOfferQuantities(matchingID);
            // check if offer has been fully resolved
            buyOfferNotResolved = BuyOfferData.getInstance().offerExists(this.getOfferID());
            if (!buyOfferNotResolved) {
                return FULLY_RESOLVED;
            }
        }
        return PARTIALLY_RESOLVED;
    }
}
