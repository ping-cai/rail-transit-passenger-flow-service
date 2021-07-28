package domain;

import java.util.Objects;

public class TransferBase {
    private String inId;
    private String outId;

    public TransferBase(String inId, String outId) {
        this.inId = inId;
        this.outId = outId;
    }

    public String getInId() {
        return inId;
    }

    public String getOutId() {
        return outId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferBase that = (TransferBase) o;
        return Objects.equals(inId, that.inId) &&
                Objects.equals(outId, that.outId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inId, outId);
    }
}
