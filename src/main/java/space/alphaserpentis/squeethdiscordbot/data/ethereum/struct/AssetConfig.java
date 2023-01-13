package space.alphaserpentis.squeethdiscordbot.data.ethereum.struct;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint24;
import org.web3j.abi.datatypes.generated.Uint32;

import java.math.BigInteger;

public class AssetConfig extends StaticStruct {
    public String address;
    public boolean borrowIsolated;
    BigInteger collateralFactor;
    BigInteger borrowFactor;
    BigInteger twapWindow;

    public AssetConfig(
            String address,
            boolean borrowIsolated,
            BigInteger collateralFactor,
            BigInteger borrowFactor,
            BigInteger twapWindow
    ) {
        super(
                new Address(address),
                new Bool(borrowIsolated),
                new Uint32(collateralFactor),
                new Uint32(borrowFactor),
                new Uint24(twapWindow)
        );

        this.address = address;
        this.borrowIsolated = borrowIsolated;
        this.collateralFactor = collateralFactor;
        this.borrowFactor = borrowFactor;
        this.twapWindow = twapWindow;
    }

    public AssetConfig(
            Address address,
            Bool borrowIsolated,
            Uint32 collateralFactor,
            Uint32 borrowFactor,
            Uint24 twapWindow
    ) {
        super(
                address,
                borrowIsolated,
                collateralFactor,
                borrowFactor,
                twapWindow
        );

        this.address = address.getValue();
        this.borrowIsolated = borrowIsolated.getValue();
        this.collateralFactor = collateralFactor.getValue();
        this.borrowFactor = borrowFactor.getValue();
        this.twapWindow = twapWindow.getValue();
    }
}
