//
// Created by Stanislav Mircic  <stanislav at backyardbrains.com>
//

#ifndef SPIKE_RECORDER_ANDROID_FILTERBASE_H
#define SPIKE_RECORDER_ANDROID_FILTERBASE_H

#include <cstdint>

//
// Base class that is inherited by all filters
//
namespace backyardbrains {

    namespace filters {

        class FilterBase {
        public:
            FilterBase();

            void initWithSamplingRate(float sr);

            void setCoefficients();

            void filter(int16_t *data, int32_t numFrames, bool flush = false);

            void filterContiguousData(float *data, int32_t numFrames);

        protected:

            void intermediateVariables(float Fc, float Q);

            float one;
            float samplingRate;
            float gInputKeepBuffer[2];
            float gOutputKeepBuffer[2];
            float omega, omegaS, omegaC, alpha;
            float coefficients[5];
            float a0, a1, a2, b0, b1, b2;
        private:
        };
    }}
#endif //SPIKE_RECORDER_ANDROID_FILTERBASE_H
