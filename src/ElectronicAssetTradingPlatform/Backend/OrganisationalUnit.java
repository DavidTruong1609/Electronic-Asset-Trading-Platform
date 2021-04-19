package ElectronicAssetTradingPlatform.Backend;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class for the organisational units which are individual departments in the organisation e.g. Human Resources or
 * Management or Public Relations etc.
 * These are the organisational units in which the users will belong in, and will use their unit's assets and credits
 * to perform exchanges on this platform
 */

public class OrganisationalUnit {
    private int organisationalUnitID;
    private String organisationalUnitName;
    private float credits;
    private Map<String,Integer> organisationalUnitAssets = new TreeMap<>();

    /**
     * Constructor for an organisational unit
     *
     * @param id ID of an organisational unit
     * @param Name The organisational unit's name
     * @param credits The number of credits the organisational unit starts with/currently owns when created
     */
    public OrganisationalUnit(int id, String Name, float credits) {
        organisationalUnitID = id;
        organisationalUnitName = Name;
        this.credits = credits;
    }

    /**
     * Given an integer of credits, add this onto the existing amount of credits an organisational unit owns
     * However, cannot reduce credits by more than the organisational unit owns (can't go negative)
     * This is the base helper function for all other methods elsewhere which add/subtract organisational unit credits
     *
     * @param credits int amount of credits to add (positive int) or remove (negative int)
     *
     * @throws Exception exception handling so that net credits cannot be less than zero
     */
    public void editCredits(float credits) throws Exception {
        if ( (this.credits += credits) < 0) {           // this code line might be incorrect and may need fixing
            throw new Exception("Cannot remove more credits than there actually are!");
        }
        else {
            this.credits += credits;
        }
    }

    /**
     * Given an existing asset object (has been created by IT admins)
     * and a quantity of the asset in integers, add the asset name as the key and quantity as
     * the value into the organisationalUnitAssets' TreeMap collection IF the organisation does not own any amount of
     * the asset yet.
     * Otherwise, add onto the existing quantity value in the TreeMap
     *
     * @param assetName Asset object which an organisational unit owns/going to own
     * @param quantityToAdd Number of that particular asset to be added (must be greater than 0)
     *
     */
    public void addAsset(String assetName, int quantityToAdd) {
        // if the organisation already has any amount of the asset


        if (organisationalUnitAssets.containsKey(assetName)) {
            int currentQuantity = organisationalUnitAssets.get(assetName);
            quantityToAdd += currentQuantity;
            organisationalUnitAssets.put(assetName, quantityToAdd);
        }
        // if the organisation does not currently have any amount of the asset
        else {
            organisationalUnitAssets.put(assetName, quantityToAdd);
        }
    }

    /**
     * Given an existing asset object (has been created by IT admins) and a quantity of the asset in integers,
     * reduce the quantity of the asset owned in the organisationalUnitAssets' TreeMap collection, where the asset name
     * is the Key and quantity is the Value.
     *
     *
     * The asset name should be unique and the quantity removed should not reduce the asset below zero
     *
     * @param assetName String asset which an organisational unit owns
     * @param quantityToRemove Number of that particular asset to be removed (must be less than number owned currently)
     *
     * @throws Exception // not enough assets owned to be removed
     * @throws Exception // organisational unit does not have the asset (cannot remove asset that is not owned)
     */

    public void removeAsset(String assetName, int quantityToRemove) throws Exception {
        int currentQuantity = organisationalUnitAssets.get(assetName);
        if (organisationalUnitAssets.containsKey(assetName)) {
            if (quantityToRemove <= currentQuantity) {
                currentQuantity -= quantityToRemove;
                organisationalUnitAssets.put(assetName, currentQuantity);
            }
            else {
                throw new Exception("Cannot remove more assets than there are currently!"); // this can be refined
            }
        }
        else {
            throw new Exception("Asset does not currently exist!");
        }
    }
}