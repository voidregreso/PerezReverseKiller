#include "rar.hpp"

#ifdef _BEOS
#include "beosea.cpp"
#endif
#if defined(_EMX) && !defined(_DJGPP)
#include "os2ea.cpp"
#endif
#ifdef _UNIX
#include "inactive/uowners.cpp"
#endif



#ifndef SFX_MODULE
void SetExtraInfo(CommandData *Cmd, Archive &Arc, char *Name, wchar *NameW) {
    switch(Arc.SubBlockHead.SubType) {
#if defined(_EMX) && !defined(_DJGPP)
    case EA_HEAD:
        if (Cmd->ProcessEA)
            ExtractOS2EA(Arc, Name);
        break;
#endif
#ifdef _UNIX
    case UO_HEAD:
        if (Cmd->ProcessOwners)
            ExtractUnixOwner(Arc, Name);
        break;
#endif
#ifdef _BEOS
    case BEEA_HEAD:
        if (Cmd->ProcessEA)
            ExtractBeEA(Arc, Name);
        break;
#endif
    }
}
#endif


void SetExtraInfoNew(CommandData *Cmd, Archive &Arc, char *Name, wchar *NameW) {
#if defined(_EMX) && !defined(_DJGPP)
    if (Cmd->ProcessEA && Arc.SubHead.CmpName(SUBHEAD_TYPE_OS2EA))
        ExtractOS2EANew(Arc, Name);
#endif
#ifdef _UNIX
    if (Cmd->ProcessOwners && Arc.SubHead.CmpName(SUBHEAD_TYPE_UOWNER))
        ExtractUnixOwnerNew(Arc, Name);
#endif
#ifdef _BEOS
    if (Cmd->ProcessEA && Arc.SubHead.CmpName(SUBHEAD_TYPE_UOWNER))
        ExtractUnixOwnerNew(Arc, Name);
#endif
#ifdef _WIN_32
    if (Cmd->ProcessOwners && Arc.SubHead.CmpName(SUBHEAD_TYPE_ACL))
        ExtractACLNew(Arc, Name, NameW);
    if (Arc.SubHead.CmpName(SUBHEAD_TYPE_STREAM))
        ExtractStreamsNew(Arc, Name, NameW);
#endif
}
