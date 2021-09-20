package com.android.fira.applet;

import com.android.ber.BerArrayLinkList;
import com.android.ber.BerTlvParser;
import javacard.framework.*;
import javacardx.apdu.ExtendedLength;

@SuppressWarnings("FieldCanBeLocal")
public class FiraApplet extends Applet implements ExtendedLength {

    private static final short KM_HAL_VERSION = (short) 0x4000;
    private static final byte CLA_ISO7816_NO_SM_NO_CHAN = (byte) 0x80;

    protected static byte[] mHeapBuffer;
    protected static short m_ExtBufLength;

    private static ADFManager mAdfManager;
    private static SessionManager mSessionManager;
    private static Repository mRepository;
    private static CryptoManager mCryptoManager;

    /**
     * Registers this applet.
     */
    protected FiraApplet() {
        mAdfManager = new ADFManager();
        mSessionManager = new SessionManager();
        mCryptoManager = new CryptoManager();
        mRepository = new Repository();
        mHeapBuffer = JCSystem.makeTransientByteArray(Constant.HEAP_SIZE, JCSystem.CLEAR_ON_RESET);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new FiraApplet().register();
    }

    @Override
    public void deselect() {
    }

    /**
     * brief This method is called whenever the applet is being selected.
     */
    @Override
    public boolean select() {
        return true;
    }

    protected void validateApduHeader(APDU apdu) {
        // Read the apdu header and buffer.
        byte[] apduBuffer = apdu.getBuffer();
        byte apduClass = apduBuffer[ISO7816.OFFSET_CLA];
        short P1P2 = Util.getShort(apduBuffer, ISO7816.OFFSET_P1);

        // Validate APDU Header.
        if ((apduClass != CLA_ISO7816_NO_SM_NO_CHAN)) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        // Validate P1P2.
        if (P1P2 != FiraApplet.KM_HAL_VERSION) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }

    /**
     * Receive extended data
     */
    public static void receiveIncoming(APDU apdu) {
        byte[] srcBuffer = apdu.getBuffer();
        short revLen = apdu.setIncomingAndReceive();
        short srcOffset = apdu.getOffsetCdata();
        m_ExtBufLength = apdu.getIncomingLength();
        short index = 0;

        while (revLen > 0 && index  < m_ExtBufLength) {
            Util.arrayCopyNonAtomic(srcBuffer, srcOffset, mHeapBuffer, index, revLen);
            index += revLen;
            revLen = apdu.receiveBytes(srcOffset);
        }
    }

    private void processLoadADF(APDU apdu) {
        byte[] adfBuff = mRepository.getADFBuffer();
        receiveIncoming(apdu);

        if (Constant.ADF_PACS_PROFILE_SIZE != mCryptoManager.aesCBC128NoPadDecrypt(mHeapBuffer,
                        mRepository.getADFBufferOffset(), m_ExtBufLength, adfBuff, (short) 0)) {
            /*TODO:- */
            return;
        }

        if (mAdfManager.parser(adfBuff, (short)0, m_ExtBufLength)) {
            /*TODO:- */
            return;
        }

        mRepository.setCurrentADF();

        /* TODO:- return response */
    }

    private void processSelectADF(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short revLen = apdu.setIncomingAndReceive();
        short srcOffset = apdu.getOffsetCdata();
        short dataLen = apdu.getIncomingLength();
        // check dataLen == revLen ??

        if (mAdfManager.parser(apduBuffer, srcOffset, revLen)) {

        }

        /*TODO:- return response*/
    }

    private void processSelect(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short revLen = apdu.setIncomingAndReceive();
        short srcOffset = apdu.getOffsetCdata();
        short dataLen = apdu.getIncomingLength();
        // check dataLen == revLen ??
        if (!mRepository.verifyAID(apduBuffer, srcOffset, dataLen)) {
            /*TODO:- return response*/
            return;
        }
    }

    @Override
    public void process(APDU apdu) {
        /* SELECT */
        if (apdu.isISOInterindustryCLA()) {
            if (selectingApplet()) {
                return;
            }
        }

        validateApduHeader(apdu);
        byte[] apduBuffer = apdu.getBuffer();
        byte apduIns = apduBuffer[ISO7816.OFFSET_INS];

        switch (apduIns) {
            case Constant.INS_SELECT:
                processSelect(apdu);
            case Constant.INS_SELECT_ADF:
                processSelectADF(apdu);
                break;
            case Constant.INS_LOAD_ADF:
                processLoadADF(apdu);
                break;
        }
    }
}
