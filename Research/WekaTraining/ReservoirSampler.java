package WekaTraining;

import moa.AbstractMOAObject;
import moa.core.InstancesHeader;
import moa.streams.InstanceStream;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Random;

/**
 * Package: semanticcompression
 * Class: ReservoirSampler
 * Description: Takes an InstanceStream and uses a reservoir sampling algorithm to randomly sample the specified number of instances from it.
 */
public class ReservoirSampler extends AbstractMOAObject implements InstanceStream {
    InstanceStream _stream;
    Instances _sampledData;
    int _numSamples, _maxInstances, _currInstance;

    public ReservoirSampler(InstanceStream stream, int numSamples, int maxInstances) {
        if (numSamples <= 0)
            throw new IllegalArgumentException("number of samples must be greater than 0");
        if (numSamples > maxInstances && maxInstances != -1)
            throw new IllegalArgumentException("Number of samples cannot be larger than max number of instances to sample from, unless -1 is supplied, which means there is not a limit on the number of instances to sample from");

        _stream = stream;
        _numSamples = numSamples;
        _maxInstances = maxInstances;

        restart();
    }

    public InstancesHeader getHeader() {
        return _stream.getHeader();
    }

    public long estimatedRemainingInstances() {
        return _numSamples - _currInstance - 1;
    }

    public boolean hasMoreInstances() {
        return _numSamples - _currInstance - 1 != 0;
    }

    public Instance nextInstance() {
        return _sampledData.get(_currInstance++);
    }

    public boolean isRestartable() {
        return true;
    }

    public void restart() {
        _currInstance = 0;
        if (_stream.isRestartable())
            _stream.restart();
        _sampledData = new Instances(_stream.getHeader(), _numSamples);
        for (int i = 0; i < _numSamples && _stream.hasMoreInstances(); i++) {
            _sampledData.add(i, _stream.nextInstance());
        }
        Random rand = new Random();
        double doubleNumSamples = (double) _numSamples;
        for (int i = _numSamples; _stream.hasMoreInstances() && (i < _maxInstances || _maxInstances < 0); i++) {
            if (rand.nextDouble() < (doubleNumSamples / i))
                _sampledData.set(rand.nextInt(_numSamples), _stream.nextInstance());
            else
                _stream.nextInstance();
        }
        _sampledData.compactify();
        _numSamples = _sampledData.numInstances();
    }

    public void getDescription(StringBuilder sb, int indent) {}
}
