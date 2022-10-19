package space.alphaserpentis.squeethdiscordbot.data.ethereum;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint96;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.osqth;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.ethUsdcPool;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.osqthEthPool;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.usdc;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.weth;

public interface CommonFunctions {
    Function getTwap_ethUsd = new Function(
            "getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(ethUsdcPool),
                    new org.web3j.abi.datatypes.Address(weth),
                    new org.web3j.abi.datatypes.Address(usdc),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );
    Function getTwap_osqth = new Function(
            "getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(osqthEthPool),
                    new org.web3j.abi.datatypes.Address(osqth),
                    new org.web3j.abi.datatypes.Address(weth),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );
    Function getVaultDetails = new Function("getVaultDetails",
            Collections.emptyList(),
            Arrays.asList(
                    new TypeReference<Address>() { },
                    new TypeReference<Uint32>() { },
                    new TypeReference<Uint96>() { },
                    new TypeReference<Uint128>() { }
            )
    );
    Function callTotalSupply = new Function("totalSupply",
            Collections.emptyList(),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );
    Function getExpectedNormFactor = new Function("getExpectedNormalizationFactor",
            List.of(),
            List.of(
                    new TypeReference<Uint256>() {}
            )
    );
}
