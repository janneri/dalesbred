package fi.evident.dalesbred.instantiation;

import java.lang.reflect.Constructor;

import static fi.evident.dalesbred.utils.Primitives.unwrap;
import static fi.evident.dalesbred.utils.Primitives.wrap;
import static java.lang.reflect.Modifier.isPublic;

/**
 * Provides {@link Instantiator}s for classes.
 */
public final class InstantiatorRegistry {

    private static final int SAME_COST = 0;
    private static final int SUBTYPE_COST = 1;
    private static final int BOXING_COST = 100;
    private static final int UNBOXING_COST = 101;
    private static final int ENUM_COST = 200;
    private static final int NO_MATCH_COST = Integer.MAX_VALUE;

    /**
     * Returns constructor matching given argument types. Differs from 
     * {@link Class#getConstructor(Class[])} in that this method allows
     * does not require strict match for types, but finds any constructor
     * that is assignable from given types.
     */
    public <T> Instantiator<T> findInstantiator(Class<T> cl, NamedTypeList types) throws NoSuchMethodException {
        Instantiator<T> best = null;

        for (Constructor<T> constructor : constructorsFor(cl)) {
            Instantiator<T> instantiator = instantiatorFrom(constructor, types);
            if (instantiator != null && (best == null || instantiator.getCost() < best.getCost()))
                best = instantiator;
        }

        if (best != null)
            return best;
        else
            throw new NoSuchMethodException(cl + " does not have constructor matching types " + types.toString());
    }

    private <T> Instantiator<T> instantiatorFrom(Constructor<T> constructor, NamedTypeList types) {
        if (!isPublic(constructor.getModifiers())) return null;
        
        int cost = cost(constructor, types);
        if (cost != NO_MATCH_COST)
            return new ConstructorInstantiator<T>(constructor, cost);
        else
            return null;
    }

    private static int cost(Constructor<?> constructor, NamedTypeList columnTypes) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        if (parameterTypes.length != columnTypes.size())
            return NO_MATCH_COST;

        int totalCost = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            int assignScore = assignmentCost(parameterTypes[i], columnTypes.getType(i));
            if (assignScore == NO_MATCH_COST)
                return NO_MATCH_COST;
            else
                totalCost += assignScore;
        }

        return totalCost;
    }

    private static int assignmentCost(Class<?> target, Class<?> source) {
        return target == source                          ? SAME_COST
             : target.isAssignableFrom(source)           ? SUBTYPE_COST
             : target.isAssignableFrom(wrap(source))     ? BOXING_COST
             : target.isAssignableFrom(unwrap(source))   ? UNBOXING_COST
             : target.isEnum()                           ? ENUM_COST
             : NO_MATCH_COST;
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T>[] constructorsFor(Class<T> cl) {
        return (Constructor<T>[]) cl.getConstructors();
    }
}
