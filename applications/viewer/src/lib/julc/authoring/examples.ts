/* Starter validators for the validator lab, in julc Java. Each compiles with julc 0.1.0-pre18. */

export interface Example {
	id: string;
	name: string;
	summary: string;
	source: string;
}

export const EXAMPLES: Example[] = [
	{
		id: 'vesting',
		name: 'Vesting',
		summary: 'Spend: the beneficiary can claim after a time; the owner can cancel at any time.',
		source: `import java.math.BigInteger;

@SpendingValidator
class Vesting {
    record VestingDatum(byte[] owner, byte[] beneficiary, BigInteger unlockAt) {}

    sealed interface Action {
        record Claim() implements Action {}
        record Cancel() implements Action {}
    }

    @Entrypoint
    static boolean validate(VestingDatum datum, Action action, ScriptContext ctx) {
        TxInfo txInfo = ctx.txInfo();
        if (action instanceof Action.Cancel cancel) {
            ContextsLib.trace("cancel: needs the owner's signature");
            return ContextsLib.signedBy(txInfo, datum.owner());
        }
        ContextsLib.trace("claim: needs the beneficiary, after unlockAt");
        boolean signed = ContextsLib.signedBy(txInfo, datum.beneficiary());
        BigInteger from = IntervalLib.finiteLowerBound(txInfo.validRange());
        return signed && from.compareTo(datum.unlockAt()) >= 0;
    }
}
`
	},
	{
		id: 'owner',
		name: 'Owner only',
		summary: 'Spend: a script parameter fixes the one key that may spend.',
		source: `@SpendingValidator
class OwnerOnly {
    @Param static byte[] owner;

    @Entrypoint
    static boolean validate(PlutusData datum, PlutusData redeemer, ScriptContext ctx) {
        boolean signed = ContextsLib.signedBy(ctx.txInfo(), owner);
        if (!signed) ContextsLib.trace("not signed by the owner");
        return signed;
    }
}
`
	},
	{
		id: 'mint',
		name: 'Signed mint',
		summary: 'Mint: minting needs the issuer’s signature; anyone may burn.',
		source: `import java.math.BigInteger;

@MintingValidator
class SignedMint {
    @Param static byte[] issuer;

    sealed interface Action {
        record Mint(BigInteger amount) implements Action {}
        record Burn() implements Action {}
    }

    @Entrypoint
    static boolean validate(Action action, ScriptContext ctx) {
        if (action instanceof Action.Mint mint) {
            if (mint.amount().compareTo(BigInteger.ZERO) <= 0) {
                ContextsLib.trace("mint amount must be positive");
                return false;
            }
            return ContextsLib.signedBy(ctx.txInfo(), issuer);
        }
        return true;
    }
}
`
	}
];
