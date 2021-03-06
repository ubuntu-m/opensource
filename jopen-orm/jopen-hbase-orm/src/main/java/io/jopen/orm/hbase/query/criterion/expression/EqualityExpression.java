package io.jopen.orm.hbase.query.criterion.expression;

import io.jopen.orm.hbase.query.criterion.Operator;
import io.jopen.orm.hbase.query.criterion.projection.AggregateProjection;

public class EqualityExpression extends Expression {

    private final Object value;

    public EqualityExpression(Operator operator, String propertyName, Object value) {
        super(operator, propertyName);
        this.value = value;
    }

    public EqualityExpression(Operator operator, String propertyName, Object value, AggregateProjection aggregateProjection) {
        super(operator, propertyName, aggregateProjection);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getPropertyName() + " " + getOperator() + " " + value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        EqualityExpression other = (EqualityExpression) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
