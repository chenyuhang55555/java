import java.util.ArrayList;

public class TxHandler {

    private UTXOPool curUtxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent
     * transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the
     * UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        curUtxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     *         (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     *         (2) the signatures on each input of {@code tx} are valid,
     *         (3) no UTXO is claimed multiple times by {@code tx},
     *         (4) all of {@code tx}s output values are non-negative, and
     *         (5) the sum of {@code tx}s input values is greater than or equal to
     *         the sum
     *         of its output
     *         values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool tmpUtxoPool = new UTXOPool(curUtxoPool);
        ArrayList<Transaction.Input> ips = tx.getInputs();
        ArrayList<Transaction.Output> ops = tx.getOutputs();
        double sumIpValue = 0;
        double sumOpValue = 0;
        // for (Transaction.Input ip : ips) {
        for (int ipIndex = 0; ipIndex < tx.numInputs(); ipIndex++) {
            Transaction.Input ip = ips.get(ipIndex);
            // (1)
            UTXO tmpUtxo = new UTXO(ip.prevTxHash, ip.outputIndex);
            if (!tmpUtxoPool.contains(tmpUtxo))
                return false;
            // (2)
            Transaction.Output op = tmpUtxoPool.getTxOutput(tmpUtxo);
            if (!Crypto.verifySignature(op.address, tx.getRawDataToSign(ipIndex),
                    ip.signature))
                return false;
            sumIpValue += op.value;
            tmpUtxoPool.removeUTXO(tmpUtxo); // (3)
        }
        // (4)
        for (Transaction.Output op : ops) {
            if (!(op.value >= 0))
                return false;
            sumOpValue += op.value;
        }
        // (5)
        if (sumIpValue < sumOpValue)
            return false;

        return true;

    }

    /**
     * Handles each epoch by receiving an unordered array of proposed
     * transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted
     * transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        while (true) {
            boolean found = false;
            for (Transaction tx : possibleTxs) {
                if (isValidTx(tx)) {
                    updateUtxoPool(tx);
                    validTxs.add(tx);
                    found = true;
                    break;
                }
            }
            if (!found) {
                break;
            }
        }
        Transaction[] txs = new Transaction[validTxs.size()];
        int i = 0;
        for (Transaction tx : validTxs) {
            txs[i++] = tx;
        }

        return txs;
    }

    public void updateUtxoPool(Transaction tx) {
        ArrayList<Transaction.Input> ips = tx.getInputs();
        ArrayList<Transaction.Output> ops = tx.getOutputs();
        for (int ipIndex = 0; ipIndex < tx.numInputs(); ipIndex++) {
            Transaction.Input ip = ips.get(ipIndex);
            UTXO tmpUtxo = new UTXO(ip.prevTxHash, ip.outputIndex);
            curUtxoPool.removeUTXO(tmpUtxo);
        }
        for (int opIndex = 0; opIndex < tx.numOutputs(); opIndex++) {
            Transaction.Output op = ops.get(opIndex);
            UTXO tmpUtxo = new UTXO(tx.getHash(), opIndex);
            curUtxoPool.addUTXO(tmpUtxo, op);
        }
    }
}
